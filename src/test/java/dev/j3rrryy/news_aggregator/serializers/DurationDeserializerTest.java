package dev.j3rrryy.news_aggregator.serializers;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import dev.j3rrryy.news_aggregator.exceptions.IntervalIsZeroException;
import dev.j3rrryy.news_aggregator.exceptions.InvalidDurationFormatException;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
        String json = "{\"duration\":\"5h1d10m\"}";
        Wrapper result = objectMapper.readValue(json, Wrapper.class);

        Duration expected = Duration.ofDays(1).plusHours(5).plusMinutes(10);
        assertThat(result.duration).isEqualTo(expected);
    }

    @Test
    void deserializeOnlyDays() throws Exception {
        String json = "{\"duration\":\"2d\"}";
        Wrapper result = objectMapper.readValue(json, Wrapper.class);

        Duration expected = Duration.ofDays(2);
        assertThat(result.duration).isEqualTo(expected);
    }

    @Test
    void deserializeOnlyHoursMinutes() throws Exception {
        String json = "{\"duration\":\"30m3h\"}";
        Wrapper result = objectMapper.readValue(json, Wrapper.class);

        Duration expected = Duration.ofHours(3).plusMinutes(30);
        assertThat(result.duration).isEqualTo(expected);
    }

    @Test
    void deserializeZeroThrows() {
        String json = "{\"duration\":\"0m\"}";
        assertThatThrownBy(() -> objectMapper.readValue(json, Wrapper.class))
                .isInstanceOf(JsonMappingException.class)
                .hasRootCauseInstanceOf(IntervalIsZeroException.class)
                .hasMessageContaining("Interval must not be zero");
    }

    @Test
    void deserializeDuplicateDaysThrows() {
        String json = "{\"duration\":\"5d7d\"}";
        assertThatThrownBy(() -> objectMapper.readValue(json, Wrapper.class))
                .isInstanceOf(JsonMappingException.class)
                .hasRootCauseInstanceOf(InvalidDurationFormatException.class)
                .hasMessageContaining("duplicate days in 5d7d");
    }

    @Test
    void deserializeDuplicateHoursThrows() {
        String json = "{\"duration\":\"2h2h\"}";
        assertThatThrownBy(() -> objectMapper.readValue(json, Wrapper.class))
                .isInstanceOf(JsonMappingException.class)
                .hasRootCauseInstanceOf(InvalidDurationFormatException.class)
                .hasMessageContaining("duplicate hours in 2h2h");
    }

    @Test
    void deserializeDuplicateMinutesThrows() {
        String json = "{\"duration\":\"10m5m\"}";
        assertThatThrownBy(() -> objectMapper.readValue(json, Wrapper.class))
                .isInstanceOf(JsonMappingException.class)
                .hasRootCauseInstanceOf(InvalidDurationFormatException.class)
                .hasMessageContaining("duplicate minutes in 10m5m");
    }

    @Test
    void deserializeGibberishThrows() {
        String json = "{\"duration\":\"xyz\"}";
        assertThatThrownBy(() -> objectMapper.readValue(json, Wrapper.class))
                .isInstanceOf(JsonMappingException.class)
                .hasRootCauseInstanceOf(InvalidDurationFormatException.class)
                .hasMessageContaining("xyz");
    }

    @Test
    void processUnitInvalidDurationThrows() throws Exception {
        DurationDeserializer deserializer = new DurationDeserializer();

        Class<?> partsClass = Class.forName(
                "dev.j3rrryy.news_aggregator.serializers.DurationDeserializer$DurationParts"
        );
        var constructor = partsClass.getDeclaredConstructor(Long.class, Long.class, Long.class);
        constructor.setAccessible(true);
        Object partsInstance = constructor.newInstance(null, null, null);

        Method processUnit = DurationDeserializer.class.getDeclaredMethod(
                "processUnit", long.class, String.class, partsClass, String.class);
        processUnit.setAccessible(true);

        assertThatThrownBy(() -> {
            try {
                processUnit.invoke(deserializer, 5L, "x", partsInstance, "5x");
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        }).isInstanceOf(InvalidDurationFormatException.class)
                .hasMessageContaining("Unexpected unit: x in 5x");
    }

    private record Wrapper(Duration duration) {

    }

}
