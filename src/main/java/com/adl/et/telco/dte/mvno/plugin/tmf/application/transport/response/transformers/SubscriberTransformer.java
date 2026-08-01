package com.adl.et.telco.dte.mvno.plugin.tmf.application.transport.response.transformers;

import com.adl.et.telco.dte.mvno.plugin.tmf.application.transport.response.entities.SubscriberResponseEntity;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.Subscriber;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SubscriberTransformer implements ResponseTransformer<Subscriber> {

    private final ObjectMapper objectMapper;

    public SubscriberTransformer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy();

        this.objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    @Override
    public Map<String, Object> transform(Subscriber entity) {

        return objectMapper.convertValue(objectMapper.convertValue(entity, SubscriberResponseEntity.class),
                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, Object.class));
    }
}
