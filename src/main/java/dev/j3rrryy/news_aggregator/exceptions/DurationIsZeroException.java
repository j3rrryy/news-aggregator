package dev.j3rrryy.news_aggregator.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DurationIsZeroException extends RuntimeException {

    public DurationIsZeroException() {
        super("Duration must not be zero");
    }

}
