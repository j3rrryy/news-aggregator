package dev.j3rrryy.news_aggregator.parser.config;

import dev.j3rrryy.news_aggregator.enums.Source;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class ParserPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(ParserPropertiesConfig.class)
            .withPropertyValues(
                    "parser.auto-parsing-enabled=false",
                    "parser.auto-parsing-interval=PT5H",
                    "parser.source-statuses.RT_RU=true",
                    "parser.source-statuses.AIF_RU=false",
                    "parser.source-statuses.SVPRESSA_RU=true",

                    "parser.configs.RT_RU.rate-limit-per-second=40",
                    "parser.configs.RT_RU.category-urls.POLITICS=5835d35ec46188a6798b493b",

                    "parser.configs.AIF_RU.rate-limit-per-second=30",
                    "parser.configs.AIF_RU.category-urls.SOCIETY=society/people",

                    "parser.configs.SVPRESSA_RU.rate-limit-per-second=10",
                    "parser.configs.SVPRESSA_RU.category-urls.SPORT=sport"
            );

    @Test
    void testProperties() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ParserProperties.class);
            ParserProperties props = context.getBean(ParserProperties.class);

            assertThat(props.isAutoParsingEnabled()).isIn(true, false);
            assertThat(props.getAutoParsingInterval()).isNotNull();
            assertThat(props.getAutoParsingInterval().toSeconds()).isGreaterThan(0);

            Map<Source, Boolean> sourceStatuses = props.getSourceStatuses();
            assertThat(sourceStatuses).isNotNull();
            assertThat(sourceStatuses.keySet()).contains(Source.RT_RU, Source.AIF_RU, Source.SVPRESSA_RU);

            Map<Source, ParserConfig> configs = props.getConfigs();
            assertThat(configs).isNotNull();
            assertThat(configs.keySet()).contains(Source.RT_RU, Source.AIF_RU, Source.SVPRESSA_RU);

            configs.values().forEach(config -> {
                assertThat(config.getRateLimitPerSecond()).isGreaterThanOrEqualTo(0);
                assertThat(config.getCategoryUrls()).isNotNull();
            });
        });
    }

    @EnableConfigurationProperties(ParserProperties.class)
    static class ParserPropertiesConfig {

    }

}
