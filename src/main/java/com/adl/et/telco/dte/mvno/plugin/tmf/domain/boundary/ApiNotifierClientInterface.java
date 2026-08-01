package com.adl.et.telco.dte.mvno.plugin.tmf.domain.boundary;

import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.Subscriber;

public interface ApiNotifierClientInterface {

    Subscriber create(Subscriber subscriber);

    void delete(String id);
}
