/*
  Copyrights 2020 Axiata Digital Labs Pvt Ltd.
  All Rights Reserved.
  <p>
  These material are unpublished, proprietary, confidential source
  code of Axiata Digital Labs Pvt Ltd (ADL) and constitute a TRADE
  SECRET of ADL.
  <p>
  ADL retains all title to and intellectual property rights in these
  materials.
 */
package com.adl.et.telco.dte.mvno.plugin.tmf.application.config;


import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.util.Collections;


@Configuration
public class SwaggerConfig {

    @Value("${app.protocol:http}")
    private String protocol;

    @Value("${app.host:localhost}")
    private String host;

    @Value("${app.port:}")
    private String port;

    @Value("${app.context.absolute:}")
    private String absoluteContext;

    @Value("${app.context.relative:}")
    private String relativeContext;

    @Value("${app.swagger.title:API Documentation}")
    private String title;

    @Value("${app.swagger.description:}")
    private String description;

    @Value("${app.swagger.version:1.0.0}")
    private String version;


    @Bean
    public OpenAPI tmfOpenAPI() {
        OpenAPI openAPI = new OpenAPI();
        openAPI.info(apiV3Info());
        Server server = new Server();
        server.setUrl(serverUrl());
        openAPI.setServers(Collections.singletonList(server));
        return openAPI;
    }

    /**
     * Absolute URL the "Try it out" calls are sent to. {@code app.host} alone is not a valid
     * server URL, swagger-ui resolves it relative to the page and the calls never reach the
     * service, so the scheme and the port are always part of the URL.
     */
    private String serverUrl() {
        String url = host.contains("://")
                ? trimTrailingSlash(host)
                : protocol + "://" + trimTrailingSlash(host) + portSuffix();
        return url + gatewayPrefix();
    }

    private String portSuffix() {
        return StringUtils.hasText(port) ? ":" + port.trim() : "";
    }

    /**
     * Controller mappings already carry {@code app.context.absolute}, so the documented paths are
     * complete for a direct call. When the service sits behind a gateway that routes on a
     * different prefix, {@code app.context.relative} holds that prefix and it has to be part of
     * the server URL instead.
     */
    private String gatewayPrefix() {
        String relative = trimTrailingSlash(relativeContext);
        if (!StringUtils.hasText(relative) || absoluteContext.startsWith(relative)) {
            return "";
        }
        return relative.startsWith("/") ? relative : "/" + relative;
    }

    private String trimTrailingSlash(String value) {
        String trimmed = value == null ? "" : value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private Info apiV3Info() {
        return new Info().title(title)
                .description(description)
                .version(version)
                .contact(new Contact()
                        .name("Admin")
                        .url("http://www.axiatadigitallabs.com")
                        .email("adl@axiatadigitallabs.com"));
    }
}
