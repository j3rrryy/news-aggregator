package dev.j3rrryy.news_aggregator.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidCursorFormatException extends RuntimeException {

    public InvalidCursorFormatException(String cursor) {
        super("Invalid cursor format: " + cursor + ". Expected format is <ISO8601 datetime>|<UUID>");
    }

}
