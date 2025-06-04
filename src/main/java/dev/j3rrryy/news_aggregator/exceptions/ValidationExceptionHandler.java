package dev.j3rrryy.news_aggregator.exceptions;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestControllerAdvice
public class ValidationExceptionHandler {

    private static final Pattern unrecognizedFieldPattern = Pattern.compile("Unrecognized field \"([^\"]+)\"");

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Map<String, String> onMissingParameter(MissingServletRequestParameterException e) {
        return Map.of(e.getParameterName(), "Parameter '" + e.getParameterName() + "' is required");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Map<String, String> onTypeMismatch(MethodArgumentTypeMismatchException e) {
        return Map.of(e.getName(), "Invalid value '" + e.getValue() + "'");
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(ConstraintViolationException.class)
    public Map<String, String> onConstraintViolation(ConstraintViolationException e) {
        return e.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        cv -> {
                            String path = cv.getPropertyPath().toString();
                            return path.contains(".")
                                    ? path.substring(path.indexOf('.') + 1)
                                    : path;
                        },
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> replacement
                ));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Map<String, String> onArgumentNotValid(MethodArgumentNotValidException e) {
        return e.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> {
                            String msg = fe.getDefaultMessage();
                            return msg != null ? msg : "";
                        },
                        (existing, replacement) -> replacement
                ));
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Map<String, String> onMessageNotReadable(HttpMessageNotReadableException e) {
        Throwable cause = e.getMostSpecificCause();

        if (cause instanceof InvalidDurationFormatException || cause instanceof IntervalIsZeroException) {
            return Map.of("autoParsingInterval", cause.getMessage());
        }

        Matcher matcher = unrecognizedFieldPattern.matcher(cause.getMessage());
        String field = "request";

        if (matcher.find()) {
            field = matcher.group(1);
        }
        return Map.of(field, "Unrecognized field");
    }

}
