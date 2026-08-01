/*
  Copyrights 2020 Axiata Digital Labs Pvt Ltd.
  All Rights Reserved.

  These material are unpublished, proprietary, confidential source
  code of Axiata Digital Labs Pvt Ltd (ADL) and constitute a TRADE
  SECRET of ADL.

  ADL retains all title to and intellectual property rights in these
  materials.

 */
package com.adl.et.telco.dte.mvno.plugin.tmf.application.alarm;

import com.adl.et.telco.dte.plugin.alarming.dto.AlarmDef;
import com.adl.et.telco.dte.plugin.alarming.services.AlarmingUtils;
import org.springframework.stereotype.Service;

@Service
public class AlarmGen {
    private final AlarmingUtils alarm;

    public AlarmGen(AlarmingUtils alarm) {
        this.alarm = alarm;
    }

    public void alert(AlarmDef.MessageType msgType, String msg) {
        alarm.alert(alarm.createAlarm(msgType, msg));
    }
}
