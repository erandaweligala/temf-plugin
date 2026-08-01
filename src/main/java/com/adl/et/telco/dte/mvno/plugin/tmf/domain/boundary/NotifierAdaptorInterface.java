package com.adl.et.telco.dte.mvno.plugin.tmf.domain.boundary;

import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.Notification;

public interface NotifierAdaptorInterface {

    void notify(Notification notification);
}
