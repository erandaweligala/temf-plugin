package com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.Map;

@Data
public class Notification {

    private String eventId;
    private OffsetDateTime eventTime;
    private String eventType;
    private String tenantId;

    private Map<String, Object> event;
}
