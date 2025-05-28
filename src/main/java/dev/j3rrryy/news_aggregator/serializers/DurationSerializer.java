package dev.j3rrryy.news_aggregator.serializers;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;
import java.time.Duration;

@JsonComponent
public class DurationSerializer extends JsonSerializer<Duration> {

    @Override
    public void serialize(
            Duration duration, JsonGenerator jsonGenerator, SerializerProvider provider
    ) throws IOException {
        long days = duration.toDays();
        long hours = duration.minusDays(days).toHours();
        long minutes = duration.minusDays(days).minusHours(hours).toMinutes();

        StringBuilder stringBuilder = new StringBuilder();
        if (days > 0) stringBuilder.append(days).append("d");
        if (hours > 0) stringBuilder.append(hours).append("h");
        if (minutes > 0) stringBuilder.append(minutes).append("m");
        if (stringBuilder.isEmpty()) stringBuilder.append("0m");

        jsonGenerator.writeString(stringBuilder.toString());
    }

}
