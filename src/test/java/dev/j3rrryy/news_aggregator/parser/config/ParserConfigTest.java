package dev.j3rrryy.news_aggregator.parser.config;

import dev.j3rrryy.news_aggregator.enums.Category;
import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class ParserConfigTest {

    @Test
    void categoryUrls_shouldInitializeEmptyEnumMap() {
        ParserConfig config = new ParserConfig();

        assertThat(config.getCategoryUrls()).isNotNull();
        assertThat(config.getCategoryUrls()).isEmpty();
        assertThat(config.getCategoryUrls()).isInstanceOf(EnumMap.class);
    }

    @Test
    void shouldSetAndGetRateLimit() {
        ParserConfig config = new ParserConfig();

        config.setRateLimitPerSecond(2.5);
        assertThat(config.getRateLimitPerSecond()).isEqualTo(2.5);
    }

    @Test
    void shouldModifyCategoryUrlsMap() {
        ParserConfig config = new ParserConfig();

        config.getCategoryUrls().put(Category.SCIENCE_TECH, Set.of("test"));
        assertThat(config.getCategoryUrls())
                .containsKey(Category.SCIENCE_TECH)
                .containsValue(Set.of("test"));
    }

}
