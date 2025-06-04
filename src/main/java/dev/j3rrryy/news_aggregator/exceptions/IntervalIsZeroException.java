package dev.j3rrryy.news_aggregator.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class IntervalIsZeroException extends RuntimeException {

    public IntervalIsZeroException() {
        super("Interval must not be zero");
    }

}
