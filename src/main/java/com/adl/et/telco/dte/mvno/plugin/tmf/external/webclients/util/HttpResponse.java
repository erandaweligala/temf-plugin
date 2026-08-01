package com.adl.et.telco.dte.mvno.plugin.tmf.external.webclients.util;

import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

@Data
public class HttpResponse<T> {

    private T body;
    private HttpStatus status;
    private HttpHeaders headers;
}
