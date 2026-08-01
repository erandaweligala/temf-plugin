package com.adl.et.telco.dte.mvno.plugin.tmf.application.transport.request.entities;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;

@Data
public class SubscriberRequestEntity {

    @NotEmpty
    private String callback;
    private String query;
}
