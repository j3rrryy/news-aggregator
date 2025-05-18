package dev.j3rrryy.news_aggregator.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class FromDateAfterToDateException extends RuntimeException {

    public FromDateAfterToDateException() {
        super("'fromDate' timestamp must be before 'toDate' timestamp");
    }

}
