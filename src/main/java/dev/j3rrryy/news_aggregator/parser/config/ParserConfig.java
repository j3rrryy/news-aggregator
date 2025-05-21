package dev.j3rrryy.news_aggregator.parser.config;

import dev.j3rrryy.news_aggregator.enums.Category;
import lombok.Getter;
import lombok.Setter;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

@Getter
@Setter
public class ParserConfig {

    private final Map<Category, Set<String>> categoryUrls = new EnumMap<>(Category.class);
    private double rateLimitPerSecond;

}
