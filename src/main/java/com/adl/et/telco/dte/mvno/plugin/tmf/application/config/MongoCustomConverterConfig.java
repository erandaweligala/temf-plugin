package com.adl.et.telco.dte.mvno.plugin.tmf.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;

@Configuration
public class MongoCustomConverterConfig {

    @Bean
    public MongoCustomConversions customConversions() {
        return new MongoCustomConversions(Arrays.asList(
                new DateTimeToLongConverter(),
                new LongToDateTimeConverter()
        ));
    }

    @WritingConverter
    static class DateTimeToLongConverter implements Converter<OffsetDateTime, Long> {

        @Override
        public Long convert(OffsetDateTime dateTime) {
            return dateTime.toInstant().toEpochMilli();
        }
    }

    @ReadingConverter
    static class LongToDateTimeConverter implements Converter<Long, OffsetDateTime> {

        @Override
        public OffsetDateTime convert(Long source) {
            return OffsetDateTime.of(LocalDateTime.ofInstant(Instant.ofEpochMilli(source) , ZoneOffset.UTC),
                    ZoneOffset.UTC);
        }
    }
}
