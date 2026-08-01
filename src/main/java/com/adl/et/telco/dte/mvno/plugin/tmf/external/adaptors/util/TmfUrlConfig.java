package com.adl.et.telco.dte.mvno.plugin.tmf.external.adaptors.util;

import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.BaseResourceDocument;

import java.util.HashMap;
import java.util.Map;

public class TmfUrlConfig {

    Map<String, String> mapping = new HashMap<>();

    public TmfUrlConfig addConfig(String entity, String context) {

        mapping.put(entity, context);

        return this;
    }

    public <T extends BaseResourceDocument> TmfUrlConfig addConfig(Class<T> entity, String context) {

        return addConfig(entity.getSimpleName(), context);
    }

    public Map<String, String> getConfigMap() {

        return mapping;
    }
}
