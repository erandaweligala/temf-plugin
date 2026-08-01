package com.adl.et.telco.dte.mvno.plugin.tmf.external.adaptors;

import ch.qos.logback.classic.Logger;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.boundary.NotifierAdaptorInterface;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.Notification;
import com.adl.et.telco.dte.plugin.logging.services.LoggingUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Objects;

@Component
public class KafkaPublisher implements NotifierAdaptorInterface {

    private static final Logger log = LoggingUtils.getLogger(KafkaPublisher.class.getName());

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final String eventTopic;
    private final Boolean enabled;

    private final HttpServletRequest servletRequest;

    public KafkaPublisher(KafkaTemplate<String, Object> kafkaTemplate,
                          @Value("${tmf.event.topic}") String eventTopic,
                          @Value("${tmf.event.enabled}") Boolean enabled, HttpServletRequest servletRequest) {

        this.kafkaTemplate = kafkaTemplate;
        this.eventTopic = eventTopic;
        this.enabled = enabled;
        this.servletRequest = servletRequest;
    }

    @Override
    public void notify(Notification notification) {
        String tenantId = servletRequest.getHeader("tenantId");
        if(Objects.nonNull(tenantId)){
            notification.setTenantId(tenantId);
        }
        if (Boolean.TRUE.equals(enabled)) {
            kafkaTemplate.send(eventTopic, notification);
            String notificationString = notification.toString();
            log.debug("NOTIFICATION_PUBLISHED : {} | {}", eventTopic, notificationString);
        }
    }
}
