package com.adl.et.telco.dte.mvno.plugin.tmf.external.webclients.util;

import lombok.Data;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;

@Data
public class HttpResponse<T> {

    private T body;
    private HttpStatusCode status;
    private HttpHeaders headers;
}
