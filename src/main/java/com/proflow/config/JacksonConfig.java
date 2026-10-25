package com.proflow.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;

@Configuration
public class JacksonConfig {

    public static class FlexibleInstantDeserializer extends JsonDeserializer<Instant> {
        @Override
        public Instant deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            String text = p.getText();
            if (text == null || text.trim().isEmpty()) {
                return null;
            }
            text = text.trim();

            // 1. Try standard Instant format (e.g. 2026-10-17T00:00:00Z)
            try {
                return Instant.parse(text);
            } catch (DateTimeParseException ignored) {
            }

            // 2. Try date-only format (e.g. 2026-10-17)
            try {
                return LocalDate.parse(text).atStartOfDay(ZoneOffset.UTC).toInstant();
            } catch (DateTimeParseException ignored) {
            }

            // 3. Try date-time without offset (e.g. 2026-10-17T00:00:00)
            try {
                return LocalDateTime.parse(text).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException ignored) {
            }

            // 4. Try Epoch Milliseconds
            try {
                long epochMilli = Long.parseLong(text);
                return Instant.ofEpochMilli(epochMilli);
            } catch (NumberFormatException ignored) {
            }

            throw new IOException("Cannot deserialize Instant from value: " + text);
        }
    }

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> builder.deserializerByType(Instant.class, new FlexibleInstantDeserializer());
    }
}
