package com.adl.et.telco.dte.mvno.plugin.tmf.domain.boundary;

import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.BaseResourceDocument;

public interface UrlResolverAdaptorInterface {

    default <T extends BaseResourceDocument> String createHref(T entity) {

        return createHref(entity.getClass(), entity.getId());
    }

    <T> String createHref(Class<T> entity, String... id);

    String createHref(String collection, String... id);
}
