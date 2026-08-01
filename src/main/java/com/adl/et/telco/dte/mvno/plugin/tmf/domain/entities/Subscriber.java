package com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities;

import lombok.Data;

@Data
public class Subscriber {

    private String id;
    private String callback;
    private String query;
    private String service;
    private String topic;
}
