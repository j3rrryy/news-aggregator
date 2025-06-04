package dev.j3rrryy.news_aggregator.serializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import dev.j3rrryy.news_aggregator.exceptions.IntervalIsZeroException;
import dev.j3rrryy.news_aggregator.exceptions.InvalidDurationFormatException;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonComponent
public class DurationDeserializer extends JsonDeserializer<Duration> {

    private static final Pattern durationPattern = Pattern.compile("(\\d+)([dhm])");

    @Override
    public Duration deserialize(JsonParser jsonParser, DeserializationContext context) throws IOException {
        String value = jsonParser.getText().trim();
        Matcher matcher = durationPattern.matcher(value);
        DurationParts parts = new DurationParts(null, null, null);

        boolean found = false;
        int totalMatchedLength = 0;
        while (matcher.find()) {
            totalMatchedLength += matcher.end() - matcher.start();
            found = true;
            processUnit(Long.parseLong(matcher.group(1)), matcher.group(2), parts, value);
        }

        if (!found || totalMatchedLength != value.length()) {
            throw new InvalidDurationFormatException(value);
        }

        Duration duration = Duration.ofDays(parts.days == null ? 0 : parts.days)
                .plusHours(parts.hours == null ? 0 : parts.hours)
                .plusMinutes(parts.minutes == null ? 0 : parts.minutes);

        if (duration.isZero()) {
            throw new IntervalIsZeroException();
        }
        return duration;
    }

    private void processUnit(long number, String unit, DurationParts parts, String rawValue) {
        switch (unit) {
            case "d" -> {
                if (parts.days != null) throw new InvalidDurationFormatException("duplicate days in " + rawValue);
                parts.days = number;
            }
            case "h" -> {
                if (parts.hours != null) throw new InvalidDurationFormatException("duplicate hours in " + rawValue);
                parts.hours = number;
            }
            case "m" -> {
                if (parts.minutes != null) throw new InvalidDurationFormatException("duplicate minutes in " + rawValue);
                parts.minutes = number;
            }
            default -> throw new InvalidDurationFormatException("Unexpected unit: " + unit + " in " + rawValue);
        }
    }

    private static class DurationParts {

        Long days;
        Long hours;
        Long minutes;

        DurationParts(Long days, Long hours, Long minutes) {
            this.days = days;
            this.hours = hours;
            this.minutes = minutes;
        }

    }

}
