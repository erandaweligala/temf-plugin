package com.adl.et.telco.dte.mvno.plugin.tmf.domain.service;

import com.adl.et.telco.dte.mvno.plugin.tmf.domain.boundary.ApiNotifierClientInterface;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.Subscriber;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class ApiNotificationService {

    private final ApiNotifierClientInterface client;
    private final String serviceName;
    private final String topic;

    public ApiNotificationService(ApiNotifierClientInterface client,
                                  @Value("${info.app.msName}") String serviceName,
                                  @Value("${tmf.event.topic}") String topic) {
        this.client = client;
        this.serviceName = serviceName;
        this.topic = topic;
    }

    public Subscriber create(Subscriber subscriber) {

        subscriber.setService(serviceName);
        subscriber.setTopic(topic);

        return client.create(subscriber);
    }

    public void delete(String id) {

        client.delete(id);
    }
}
