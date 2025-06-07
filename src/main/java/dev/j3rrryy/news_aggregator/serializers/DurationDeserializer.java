package dev.j3rrryy.news_aggregator.serializers;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.google.common.annotations.VisibleForTesting;
import dev.j3rrryy.news_aggregator.exceptions.IntervalIsZeroException;
import dev.j3rrryy.news_aggregator.exceptions.InvalidDurationFormatException;
import org.apache.logging.log4j.util.TriConsumer;
import org.springframework.boot.jackson.JsonComponent;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@JsonComponent
public class DurationDeserializer extends JsonDeserializer<Duration> {

    private static final Pattern durationPattern = Pattern.compile("(\\d+)([dhm])");
    private static final Map<String, TriConsumer<DurationParts, Long, String>> unitHandlers =
            Map.of(
                    "d", (parts, value, rawValue) -> {
                        if (parts.days != null)
                            throw new InvalidDurationFormatException("duplicate days in " + rawValue);
                        parts.days = value;
                    },
                    "h", (parts, value, rawValue) -> {
                        if (parts.hours != null)
                            throw new InvalidDurationFormatException("duplicate hours in " + rawValue);
                        parts.hours = value;
                    },
                    "m", (parts, value, rawValue) -> {
                        if (parts.minutes != null)
                            throw new InvalidDurationFormatException("duplicate minutes in " + rawValue);
                        parts.minutes = value;
                    }
            );

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
            TriConsumer<DurationParts, Long, String> handler = unitHandlers.get(matcher.group(2));
            handler.accept(parts, Long.parseLong(matcher.group(1)), value);
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

    @VisibleForTesting
    static class DurationParts {

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
