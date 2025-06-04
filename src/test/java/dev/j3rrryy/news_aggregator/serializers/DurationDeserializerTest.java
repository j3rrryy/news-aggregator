package dev.j3rrryy.news_aggregator.serializers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import dev.j3rrryy.news_aggregator.exceptions.DurationIsZeroException;
import dev.j3rrryy.news_aggregator.exceptions.InvalidDurationFormatException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class DurationDeserializerTest {

    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setup() {
        SimpleModule module = new SimpleModule();
        module.addDeserializer(Duration.class, new DurationDeserializer());

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(module);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    void deserializeDaysHoursMinutes() throws Exception {
        String json = "{\"duration\":\"1d5h10m\"}";
        Wrapper result = objectMapper.readValue(json, Wrapper.class);

        Duration expected = Duration.ofDays(1).plusHours(5).plusMinutes(10);
        assertThat(result.duration).isEqualTo(expected);
    }

    @Test
    void deserializeOnlyDays() throws Exception {
        String json = "{\"duration\":\"2d0h0m\"}";
        Wrapper result = objectMapper.readValue(json, Wrapper.class);

        Duration expected = Duration.ofDays(2);
        assertThat(result.duration).isEqualTo(expected);
    }

    @Test
    void deserializeOnlyHoursMinutes() throws Exception {
        String json = "{\"duration\":\"0d3h30m\"}";
        Wrapper result = objectMapper.readValue(json, Wrapper.class);

        Duration expected = Duration.ofHours(3).plusMinutes(30);
        assertThat(result.duration).isEqualTo(expected);
    }

    @Test
    void deserializeZeroThrows() {
        String json = "{\"duration\":\"0d0h0m\"}";
        assertThatThrownBy(() -> objectMapper.readValue(json, Wrapper.class))
                .isInstanceOf(JsonMappingException.class)
                .hasRootCauseInstanceOf(DurationIsZeroException.class);
    }

    @Test
    void deserializeInvalidFormatThrows() {
        String json = "{\"duration\":\"3d5m\"}";
        assertThatThrownBy(() -> objectMapper.readValue(json, Wrapper.class))
                .isInstanceOf(JsonMappingException.class)
                .hasRootCauseInstanceOf(InvalidDurationFormatException.class)
                .hasMessageContaining("3d5m");
    }

    @Test
    void deserializeGibberishThrows() {
        String json = "{\"duration\":\"xyz\"}";
        assertThatThrownBy(() -> objectMapper.readValue(json, Wrapper.class))
                .isInstanceOf(JsonMappingException.class)
                .hasRootCauseInstanceOf(InvalidDurationFormatException.class)
                .hasMessageContaining("xyz");
    }

    record Wrapper(Duration duration) {

    }

}
