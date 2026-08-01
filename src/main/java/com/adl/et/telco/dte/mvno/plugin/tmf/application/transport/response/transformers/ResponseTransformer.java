package com.adl.et.telco.dte.mvno.plugin.tmf.application.transport.response.transformers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public interface ResponseTransformer<T> {

    Map<String, Object> transform(T entity);

    default Map<String, Object> transform(T entity, List<String> fields) {

        Map<String, Object> mapped = transform(entity);

        Map<String, Object> filtered = new HashMap<>();

        fields.forEach(field -> {
            if (mapped.containsKey(field)) {
                filtered.put(field, mapped.get(field));
            }
        });

        return filtered;
    }
}
