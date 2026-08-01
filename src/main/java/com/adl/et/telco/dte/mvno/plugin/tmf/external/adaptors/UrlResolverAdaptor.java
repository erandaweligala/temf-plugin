package com.adl.et.telco.dte.mvno.plugin.tmf.external.adaptors;

import com.adl.et.telco.dte.mvno.plugin.tmf.application.config.TmfPluginConfiguration;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.boundary.UrlResolverAdaptorInterface;
import com.adl.et.telco.dte.mvno.plugin.tmf.external.adaptors.util.TmfUrlConfig;
import com.adl.et.telco.dte.mvno.plugin.tmf.external.exception.AdaptorException;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Arrays;
import java.util.Map;

@Component
public class UrlResolverAdaptor implements UrlResolverAdaptorInterface {

    private final Map<String, String> mapping;
    private final String baseUrl;

    public UrlResolverAdaptor(
            TmfPluginConfiguration configuration,
            TmfUrlConfig config) {

            this.baseUrl = UriComponentsBuilder.newInstance()
                    .scheme(configuration.getProtocol())
                    .host(configuration.getHost())
                    .port(configuration.getPort())
                    .path(configuration.getContext().getRelative()).toUriString() + "/";

        mapping = config.getConfigMap();
    }

    @Override
    public <T> String createHref(Class<T> entity, String... id) {

        if (mapping.containsKey(entity.getSimpleName())) {
            return baseUrl + mapping.get(entity.getSimpleName()) + "/" +
                    Arrays.stream(id).reduce((s1, s2) -> s1 + "/" + s2).orElse("");
        }

        throw new AdaptorException("Resource context is not registered for class " + entity.getSimpleName());
    }

    @Override
    public String createHref(String collection, String... id) {

        if (mapping.containsKey(collection)) {
            return baseUrl + mapping.get(collection) + "/" +
                    Arrays.stream(id).reduce((s1, s2) -> s1 + "/" + s2).orElse("");
        }

        throw new AdaptorException("Resource context is not registered for collection " + collection);
    }
}
