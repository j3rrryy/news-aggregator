package dev.j3rrryy.news_aggregator.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class ParsingNotRunningException extends RuntimeException {

    public ParsingNotRunningException() {
        super("Parsing is not running");
    }

}
