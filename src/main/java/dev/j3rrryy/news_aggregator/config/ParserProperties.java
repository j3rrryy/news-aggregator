package dev.j3rrryy.news_aggregator.config;

import dev.j3rrryy.news_aggregator.enums.Source;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.EnumMap;
import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties("parser")
public class ParserProperties {

    private Map<Source, Boolean> sourceStatus = new EnumMap<>(Source.class);

}
