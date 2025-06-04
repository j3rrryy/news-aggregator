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

        Long days = null;
        Long hours = null;
        Long minutes = null;

        boolean found = false;
        int totalMatchedLength = 0;
        while (matcher.find()) {
            totalMatchedLength += matcher.end() - matcher.start();
            found = true;

            long number = Long.parseLong(matcher.group(1));
            String unit = matcher.group(2);
            switch (unit) {
                case "d" -> {
                    if (days != null) throw new InvalidDurationFormatException("duplicate days in " + value);
                    days = number;
                }
                case "h" -> {
                    if (hours != null) throw new InvalidDurationFormatException("duplicate hours in " + value);
                    hours = number;
                }
                case "m" -> {
                    if (minutes != null) throw new InvalidDurationFormatException("duplicate minutes in " + value);
                    minutes = number;
                }
                default -> throw new InvalidDurationFormatException(value);
            }
        }

        if (!found || totalMatchedLength != value.length()) {
            throw new InvalidDurationFormatException(value);
        }

        Duration duration = Duration.ofDays(days == null ? 0 : days)
                .plusHours(hours == null ? 0 : hours)
                .plusMinutes(minutes == null ? 0 : minutes);

        if (duration.isZero()) {
            throw new IntervalIsZeroException();
        }
        return duration;
    }

}
