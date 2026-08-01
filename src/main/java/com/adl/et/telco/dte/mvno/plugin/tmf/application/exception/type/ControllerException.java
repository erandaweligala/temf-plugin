/*
  Copyrights 2020 Axiata Digital Labs Pvt Ltd.
  All Rights Reserved.

  These material are unpublished, proprietary, confidential source
  code of Axiata Digital Labs Pvt Ltd (ADL) and constitute a TRADE
  SECRET of ADL.

  ADL retains all title to and intellectual property rights in these
  materials.

 */
package com.adl.et.telco.dte.mvno.plugin.tmf.application.exception.type;

public class ControllerException extends BaseException {
    public ControllerException(String message){
        super(message);
    }
    public ControllerException(String message, String code){
        super(message,code);
    }
}
