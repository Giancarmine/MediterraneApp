package com.ges.mediterraneapp.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.module.SimpleModule;

/**
 * Keeps the historical JSON representation of {@link HttpStatus}.
 *
 * <p>Boot 2.2 ran on Jackson 2, which serialized the {@code HttpStatus} enum with
 * {@code name()}. Jackson 3 serializes enums with {@code toString()}, which would turn
 * the {@code /recipe/status} body into {@code "418 I_AM_A_TEAPOT"} instead of
 * {@code "I_AM_A_TEAPOT"}. Registering an explicit serializer preserves the public API.
 */
@Configuration
public class JacksonConfig {

    @Bean
    JsonMapperBuilderCustomizer httpStatusNameCustomizer() {
        return builder -> builder.addModule(new SimpleModule("httpStatusName")
                .addSerializer(HttpStatus.class, new ValueSerializer<>() {
                    @Override
                    public void serialize(HttpStatus value, JsonGenerator generator,
                                          SerializationContext context) {
                        generator.writeString(value.name());
                    }
                }));
    }
}
