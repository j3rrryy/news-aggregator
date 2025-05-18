package dev.j3rrryy.news_aggregator.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ArticleNotFoundException extends RuntimeException {

    public ArticleNotFoundException(UUID id) {
        super("Article " + id + " not found");
    }

}
