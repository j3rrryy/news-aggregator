package dev.j3rrryy.news_aggregator.utils;

import dev.j3rrryy.news_aggregator.entity.NewsArticle;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.io.OutputStream;

@FunctionalInterface
public interface Exporter {

    void accept(Specification<NewsArticle> spec, Sort sort, OutputStream outputStream, boolean includeContent);

}
