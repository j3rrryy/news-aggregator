package dev.j3rrryy.news_aggregator.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ExportFailedException extends RuntimeException {

    public ExportFailedException(String format) {
        super("Export to " + format + " failed");
    }

}
