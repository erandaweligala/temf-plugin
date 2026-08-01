package com.adl.et.telco.dte.mvno.plugin.tmf.domain.notification;

import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.BaseResourceDocument;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.Notification;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.PatchDef;

import java.util.List;
import java.util.Map;

public interface ResourceNotificationCreator<T extends BaseResourceDocument> {

    List<Notification> createNotifications(T entity);

    List<Notification> updateNotifications(Map<String, Object> changeReq, T original, T merged, T updated);

    List<Notification> updateNotifications(List<PatchDef> changeReq, T original, T merged, T updated);

    List<Notification> deleteNotifications(T entity);
}
