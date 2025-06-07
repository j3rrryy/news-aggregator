package dev.j3rrryy.news_aggregator.exceptions;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ValidationExceptionHandlerTest {

    private ValidationExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new ValidationExceptionHandler();
    }

    @Test
    void onMissingParameter_shouldReturnFieldMap() {
        MissingServletRequestParameterException e =
                new MissingServletRequestParameterException("test name", "test type");
        Map<String, String> result = handler.onMissingParameter(e);
        assertThat(result).containsEntry("test name", "Parameter 'test name' is required");
    }

    @Test
    void onTypeMismatch_shouldReturnInvalidValue() {
        MethodArgumentTypeMismatchException e =
                new MethodArgumentTypeMismatchException("test value", String.class, "test name", mock(), null);
        Map<String, String> result = handler.onTypeMismatch(e);
        assertThat(result).containsEntry("test name", "Invalid value 'test value'");
    }

    @Test
    void onConstraintViolation_shouldMapErrors() {
        Path path = mock(Path.class);
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);

        when(path.toString()).thenReturn("dto.test field");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("test message");

        ConstraintViolationException e = new ConstraintViolationException(Set.of(violation));

        Map<String, String> result = handler.onConstraintViolation(e);

        assertThat(result).containsEntry("test field", "test message");
    }

    @Test
    void onConstraintViolation_singleViolation_noDotInPath() {
        Path path = mock(Path.class);
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);

        when(path.toString()).thenReturn("test field");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("test message");

        ConstraintViolationException e = new ConstraintViolationException(Set.of(violation));

        Map<String, String> result = handler.onConstraintViolation(e);

        assertThat(result).containsExactly(Map.entry("test field", "test message"));
    }

    @Test
    void onConstraintViolation_duplicateKeys_mergesKeepingReplacement() {
        Path path1 = mock(Path.class);
        ConstraintViolation<?> violation1 = mock(ConstraintViolation.class);
        when(path1.toString()).thenReturn("dto.test field");
        when(violation1.getPropertyPath()).thenReturn(path1);
        when(violation1.getMessage()).thenReturn("test message 1");

        Path path2 = mock(Path.class);
        ConstraintViolation<?> violation2 = mock(ConstraintViolation.class);
        when(path2.toString()).thenReturn("dto.test field");
        when(violation2.getPropertyPath()).thenReturn(path2);
        when(violation2.getMessage()).thenReturn("test message 2");

        ConstraintViolationException e = new ConstraintViolationException(
                new LinkedHashSet<>(List.of(violation1, violation2))
        );

        Map<String, String> result = handler.onConstraintViolation(e);

        assertThat(result).isNotNull().isNotEmpty().containsKey("test field");
    }

    @Test
    void onArgumentNotValid_shouldMapFieldErrors() {
        BindingResult bindingResult = mock(BindingResult.class);
        MethodArgumentNotValidException e = mock(MethodArgumentNotValidException.class);
        FieldError fieldError = new FieldError("test dto", "test name", "test message");

        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        when(e.getBindingResult()).thenReturn(bindingResult);

        Map<String, String> result = handler.onArgumentNotValid(e);

        assertThat(result).containsEntry("test name", "test message");
    }

    @Test
    @SuppressWarnings("ConstantConditions")
    void onArgumentNotValid_shouldUseEmptyStringWhenDefaultMessageIsNull() {
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError fieldError = new FieldError("test dto", "test field", null);
        MethodArgumentNotValidException e = mock(MethodArgumentNotValidException.class);

        when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));
        when(e.getBindingResult()).thenReturn(bindingResult);

        Map<String, String> result = handler.onArgumentNotValid(e);

        assertThat(result).containsExactly(Map.entry("test field", ""));
    }

    @Test
    void onArgumentNotValid_duplicateFields_keepsReplacementMessage() {
        FieldError firstError =
                new FieldError("test dto", "test field", "test message 1");
        FieldError secondError =
                new FieldError("test dto", "test field", "test message 2");

        BindingResult bindingResult = mock(BindingResult.class);
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);

        when(bindingResult.getFieldErrors()).thenReturn(List.of(firstError, secondError));
        when(ex.getBindingResult()).thenReturn(bindingResult);

        Map<String, String> result = handler.onArgumentNotValid(ex);

        assertThat(result).isNotNull().isNotEmpty().containsKey("test field");
    }

    @Test
    void onMessageNotReadable_withInvalidDuration_shouldReturnMappedField() {
        InvalidDurationFormatException cause = new InvalidDurationFormatException("test duration");
        HttpMessageNotReadableException e = new HttpMessageNotReadableException("Bad request", cause, mock());

        Map<String, String> result = handler.onMessageNotReadable(e);

        assertThat(result).containsEntry("autoParsingInterval",
                "Invalid duration format: test duration. Expected a combination of numbers with units: " +
                        "'d' (days), 'h' (hours), 'm' (minutes). Examples: 2d5h, 30m7h, 3d."
        );
    }

    @Test
    void onMessageNotReadable_withUnrecognizedField_shouldExtractFieldName() {
        String msg = "Unrecognized field \"test field\"";
        Throwable cause = new RuntimeException(msg);
        HttpMessageNotReadableException e = new HttpMessageNotReadableException("Bad request", cause, mock());

        Map<String, String> result = handler.onMessageNotReadable(e);

        assertThat(result).containsEntry("test field", "Unrecognized field");
    }

    @Test
    void onMessageNotReadable_withNoMatch_shouldReturnGenericField() {
        Throwable cause = new RuntimeException("test message");
        HttpMessageNotReadableException e = new HttpMessageNotReadableException("Bad request", cause, mock());

        Map<String, String> result = handler.onMessageNotReadable(e);

        assertThat(result).containsEntry("request", "Unrecognized field");
    }

}
