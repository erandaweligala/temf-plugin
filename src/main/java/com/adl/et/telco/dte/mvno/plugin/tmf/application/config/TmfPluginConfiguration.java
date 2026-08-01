package com.adl.et.telco.dte.mvno.plugin.tmf.application.config;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
@Setter
@Getter
public class TmfPluginConfiguration {

    private String host;
    private String protocol;
    private String port;
    private ContextConfiguration context;
    private SwaggerConfiguration swagger;

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    @Setter
    public static class ContextConfiguration {
        private String absolute;
        private String relative;
    }

    @NoArgsConstructor(access = AccessLevel.PRIVATE)
    @Getter
    @Setter
    public static class SwaggerConfiguration {
        private String title;
        private String description;
        private String version;
    }
}
