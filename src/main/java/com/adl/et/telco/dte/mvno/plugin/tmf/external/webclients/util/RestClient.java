package com.adl.et.telco.dte.mvno.plugin.tmf.external.webclients.util;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RestClient {

    private final RestTemplate restTemplate;

    public RestClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public <T> HttpResponse<T> getRequest(String url, HttpHeaders headers, Class<T> responseType) {

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, responseType);

        return createResponse(response);
    }

    public <T> HttpResponse<T> getRequest(String url, HttpHeaders headers, ParameterizedTypeReference<T> responseType) {

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.GET, requestEntity, responseType);

        return createResponse(response);
    }

    public <T> HttpResponse<T> postRequest(String url, Object requestBody, HttpHeaders headers, Class<T> responseType) {

        HttpEntity<Object> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, responseType);

        return createResponse(response);
    }

    public <T> HttpResponse<T> postRequest(String url, Object requestBody, HttpHeaders headers, ParameterizedTypeReference<T> responseType) {

        HttpEntity<Object> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.POST, requestEntity, responseType);

        return createResponse(response);
    }

    public <T> HttpResponse<T> deleteRequest(String url, HttpHeaders headers, Class<T> responseType) {

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, responseType);

        return createResponse(response);
    }

    public <T> HttpResponse<T> deleteRequest(String url, HttpHeaders headers, ParameterizedTypeReference<T> responseType) {

        HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, responseType);

        return createResponse(response);
    }

    public <T> HttpResponse<T> patchRequest(String url, Object requestBody, HttpHeaders headers, Class<T> responseType) {

        HttpEntity<Object> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.PATCH, requestEntity, responseType);

        return createResponse(response);
    }

    public <T> HttpResponse<T> patchRequest(String url, Object requestBody, HttpHeaders headers, ParameterizedTypeReference<T> responseType) {

        HttpEntity<Object> requestEntity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.PATCH, requestEntity, responseType);

        return createResponse(response);
    }

    private <T> HttpResponse<T> createResponse(ResponseEntity<T> responseEntity) {

        HttpResponse<T> formattedResponse = new HttpResponse<>();

        formattedResponse.setStatus(responseEntity.getStatusCode());
        formattedResponse.setHeaders(responseEntity.getHeaders());
        formattedResponse.setBody(responseEntity.getBody());

        return formattedResponse;
    }
}
