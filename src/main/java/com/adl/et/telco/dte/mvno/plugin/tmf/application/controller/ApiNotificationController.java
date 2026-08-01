package com.adl.et.telco.dte.mvno.plugin.tmf.application.controller;

import com.adl.et.telco.dte.mvno.plugin.tmf.application.transport.request.entities.SubscriberRequestEntity;
import com.adl.et.telco.dte.mvno.plugin.tmf.application.transport.response.transformers.ResponseTransformer;
import com.adl.et.telco.dte.mvno.plugin.tmf.application.validator.RequestEntityValidator;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.Subscriber;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.service.ApiNotificationService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("${app.context.absolute}/hub")
public class ApiNotificationController {

    private final RequestEntityValidator validator;
    private final ResponseTransformer<Subscriber> transformer;
    private final ApiNotificationService service;
    private final ObjectMapper objectMapper;

    public ApiNotificationController(RequestEntityValidator validator,
                                     ResponseTransformer<Subscriber> transformer,
                                     ApiNotificationService service) {
        this.validator = validator;
        this.transformer = transformer;
        this.service = service;
        this.objectMapper = new ObjectMapper().setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .findAndRegisterModules();
    }

    /**
     * Creating new subscriber for API notifications.
     *
     * @param requestEntity Request entity of the new subscriber.
     * @return Created resource.
     */
    @PostMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> create(@RequestBody SubscriberRequestEntity requestEntity) {

        validator.validate(requestEntity);

        Subscriber entity = objectMapper.convertValue(requestEntity, Subscriber.class);

        entity = service.create(entity);

        return ResponseEntity.status(HttpStatus.CREATED).body(transformer.transform(entity));
    }

    /**
     * Delete subscriber using ID of the resource.
     *
     * @param id ID of the resource.
     * @return Response with no content.
     */
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> delete(@PathVariable String id) {

        service.delete(id);

        return ResponseEntity.noContent().build();
    }
}
