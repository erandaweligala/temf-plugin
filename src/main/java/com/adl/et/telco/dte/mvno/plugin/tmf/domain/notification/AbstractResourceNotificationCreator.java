package com.adl.et.telco.dte.mvno.plugin.tmf.domain.notification;

import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.BaseResourceDocument;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.Notification;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.PatchDef;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public abstract class AbstractResourceNotificationCreator<T extends BaseResourceDocument>
        implements ResourceNotificationCreator<T> {

    private static final String CREATE_EVENT_POSTFIX = "CreateEvent";
    private static final String DELETE_EVENT_POSTFIX = "DeleteEvent";

    private final String eventPayloadName;
    private final String eventTypeName;
    private final ObjectMapper mapper;

    protected AbstractResourceNotificationCreator(String eventPayloadName,
                                                  String eventTypeName,
                                                  ObjectMapper mapper) {

        this.eventPayloadName = eventPayloadName;
        this.eventTypeName = eventTypeName;
        this.mapper = mapper;
    }

    @Override
    public List<Notification> createNotifications(T entity) {

        return Collections.singletonList(createNotification(eventTypeName + CREATE_EVENT_POSTFIX,
                entity));
    }

    @Override
    public List<Notification> deleteNotifications(T entity) {

        return Collections.singletonList(createNotification(eventTypeName + DELETE_EVENT_POSTFIX,
                entity));
    }

    @Override
    public List<Notification> updateNotifications(Map<String, Object> changeReq, T original, T merged, T updated) {

        return Collections.emptyList();
    }

    @Override
    public List<Notification> updateNotifications(List<PatchDef> changeReq, T original, T merged, T updated) {

        return Collections.emptyList();
    }

    protected String generateEventId() {
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

        String formattedDate = myDateObj.format(myFormatObj);
        return (new Random().nextInt((999999 - 100000) + 1) + 100000) + formattedDate;
    }

    protected Notification createNotification(String eventType, T entity) {

        Notification notification = new Notification();

        notification.setEventId(generateEventId());
        notification.setEventTime(OffsetDateTime.now());
        notification.setEventType(eventType);

        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put(eventPayloadName, mapper.convertValue(entity, Map.class));
        notification.setEvent(eventPayload);

        return notification;
    }
}
