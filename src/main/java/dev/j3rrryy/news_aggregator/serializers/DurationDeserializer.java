package dev.j3rrryy.news_aggregator.serializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import dev.j3rrryy.news_aggregator.exceptions.DurationIsZeroException;
import dev.j3rrryy.news_aggregator.exceptions.InvalidDurationFormatException;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonComponent
public class DurationDeserializer extends JsonDeserializer<Duration> {

    private static final Pattern durationPattern = Pattern.compile("(\\d+)d(\\d+)h(\\d+)m");

    @Override
    public Duration deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        String value = p.getText().trim();
        Matcher matcher = durationPattern.matcher(value);

        if (matcher.matches()) {
            long days = Long.parseLong(matcher.group(1));
            long hours = Long.parseLong(matcher.group(2));
            long minutes = Long.parseLong(matcher.group(3));

            Duration duration = Duration.ofDays(days).plusHours(hours).plusMinutes(minutes);
            if (duration.isZero()) throw new DurationIsZeroException();

            return duration;
        } else {
            throw new InvalidDurationFormatException(value);
        }
    }

}
