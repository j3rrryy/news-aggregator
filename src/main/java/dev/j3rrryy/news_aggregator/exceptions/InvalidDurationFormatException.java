package dev.j3rrryy.news_aggregator.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidDurationFormatException extends RuntimeException {

    public InvalidDurationFormatException(String duration) {
        super("Invalid duration format: " + duration + ". Expected format is XdYhZm.");
    }

}
