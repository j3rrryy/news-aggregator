package dev.j3rrryy.news_aggregator.parser.service;

import dev.j3rrryy.news_aggregator.enums.Category;
import dev.j3rrryy.news_aggregator.enums.Source;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ParsingStateStore {

    private final StringRedisTemplate redisTemplate;

    public Optional<Integer> getCurrentPage(Source source, Category category, String path) {
        String key = createKey(source, category, path);
        String stored = redisTemplate.opsForValue().get(key);
        if (stored == null) return Optional.empty();
        return Optional.of(Integer.parseInt(stored));
    }

    public void updateCurrentPage(Source source, Category category, String path, int nextPage) {
        String key = createKey(source, category, path);
        redisTemplate.opsForValue().set(key, String.valueOf(nextPage));
    }

    public void clearState(Source source, Category category, String path) {
        String key = createKey(source, category, path);
        redisTemplate.delete(key);
    }

    private String createKey(Source source, Category category, String path) {
        String safePath = URLEncoder.encode(path, StandardCharsets.UTF_8);
        return "state:" + source.name() + ":" + category.name() + ":" + safePath;
    }

}
