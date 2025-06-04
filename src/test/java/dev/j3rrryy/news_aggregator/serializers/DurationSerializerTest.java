package dev.j3rrryy.news_aggregator.serializers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class DurationSerializerTest {

    private static ObjectMapper objectMapper;

    @BeforeAll
    static void setup() {
        SimpleModule module = new SimpleModule();
        module.addSerializer(Duration.class, new DurationSerializer());

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(module);
    }

    @Test
    void serializeZeroDuration() throws Exception {
        Wrapper dto = new Wrapper(Duration.ZERO);
        String json = objectMapper.writeValueAsString(dto);
        assertThat(json).isEqualTo("{\"duration\":\"0m\"}");
    }

    @Test
    void serializeOnlyMinutes() throws Exception {
        Wrapper dto = new Wrapper(Duration.ofMinutes(45));
        String json = objectMapper.writeValueAsString(dto);
        assertThat(json).isEqualTo("{\"duration\":\"45m\"}");
    }

    @Test
    void serializeOnlyHours() throws Exception {
        Wrapper dto = new Wrapper(Duration.ofHours(3));
        String json = objectMapper.writeValueAsString(dto);
        assertThat(json).isEqualTo("{\"duration\":\"3h\"}");
    }

    @Test
    void serializeHoursAndMinutes() throws Exception {
        Wrapper dto = new Wrapper(Duration.ofHours(2).plusMinutes(30));
        String json = objectMapper.writeValueAsString(dto);
        assertThat(json).isEqualTo("{\"duration\":\"2h30m\"}");
    }

    @Test
    void serializeDaysOnly() throws Exception {
        Wrapper dto = new Wrapper(Duration.ofDays(2));
        String json = objectMapper.writeValueAsString(dto);
        assertThat(json).isEqualTo("{\"duration\":\"2d\"}");
    }

    @Test
    void serializeDaysHoursMinutes() throws Exception {
        Wrapper dto = new Wrapper(Duration.ofDays(1).plusHours(5).plusMinutes(10));
        String json = objectMapper.writeValueAsString(dto);
        assertThat(json).isEqualTo("{\"duration\":\"1d5h10m\"}");
    }

    @Test
    void serializeHoursZeroMinutes() throws Exception {
        Wrapper dto = new Wrapper(Duration.ofDays(1).plusHours(3));
        String json = objectMapper.writeValueAsString(dto);
        assertThat(json).isEqualTo("{\"duration\":\"1d3h\"}");
    }

    @Test
    void serializeDaysZeroHoursMinutes() throws Exception {
        Wrapper dto = new Wrapper(Duration.ofDays(2));
        String json = objectMapper.writeValueAsString(dto);
        assertThat(json).isEqualTo("{\"duration\":\"2d\"}");
    }

    private record Wrapper(Duration duration) {

    }

}
