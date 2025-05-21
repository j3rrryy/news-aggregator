package dev.j3rrryy.news_aggregator.parser.config;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import dev.j3rrryy.news_aggregator.enums.Source;
import dev.j3rrryy.news_aggregator.serializers.DurationDeserializer;
import dev.j3rrryy.news_aggregator.serializers.DurationSerializer;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

@Getter
@Setter
@Configuration
@ConfigurationProperties("parser")
public class ParserProperties {

    private boolean autoParsingEnabled;

    @JsonSerialize(using = DurationSerializer.class)
    @JsonDeserialize(using = DurationDeserializer.class)
    private Duration autoParsingInterval;

    private Map<Source, Boolean> sourceStatuses = new EnumMap<>(Source.class);

    private Map<Source, ParserConfig> configs = new EnumMap<>(Source.class);

}
