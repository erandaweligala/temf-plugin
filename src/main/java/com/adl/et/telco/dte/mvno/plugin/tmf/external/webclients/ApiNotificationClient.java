package com.adl.et.telco.dte.mvno.plugin.tmf.external.webclients;

import com.adl.et.telco.dte.mvno.plugin.tmf.domain.boundary.ApiNotifierClientInterface;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.Subscriber;
import com.adl.et.telco.dte.mvno.plugin.tmf.external.exception.WebClientException;
import com.adl.et.telco.dte.mvno.plugin.tmf.external.webclients.util.RestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApiNotificationClient implements ApiNotifierClientInterface {

    private final RestClient restClient;
    private final String apiNotifierPath;

    public ApiNotificationClient(RestClient restClient, @Value("${tmf.services.apiNotifier.url}") String basePath) {
        this.restClient = restClient;
        this.apiNotifierPath = basePath + "/hub";
    }

    @Override
    public Subscriber create(Subscriber subscriber) {

        try {
            return restClient.postRequest(apiNotifierPath, subscriber, null, Subscriber.class)
                    .getBody();
        } catch (Exception e) {
            throw new WebClientException("Error creating subscriber", "ERROR_SUBSCRIBER_CREATE", e);
        }
    }

    @Override
    public void delete(String id) {
        String deleteUrl = String.format("%s/%s", apiNotifierPath, id);
        try {
            restClient.deleteRequest(deleteUrl, null, Void.class);
        } catch (Exception e) {
            throw new WebClientException("Error creating subscriber", "ERROR_SUBSCRIBER_CREATE", e);
        }
    }
}
