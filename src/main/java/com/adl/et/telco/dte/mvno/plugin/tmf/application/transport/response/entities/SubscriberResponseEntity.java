package com.adl.et.telco.dte.mvno.plugin.tmf.application.transport.response.entities;

import lombok.Data;

@Data
public class SubscriberResponseEntity {

    private String id;
    private String callback;
    private String query;
}
