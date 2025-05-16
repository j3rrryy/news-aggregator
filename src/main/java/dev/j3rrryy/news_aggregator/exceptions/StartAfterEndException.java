package dev.j3rrryy.news_aggregator.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class StartAfterEndException extends RuntimeException {

    public StartAfterEndException() {
        super("Start must be before end");
    }

}
