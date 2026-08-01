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

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class CircuitBreakerException extends BaseException {
    public CircuitBreakerException(String message) {
        super(message);
    }

    public CircuitBreakerException(String message, String code) {
        super(message, code);
    }
    public CircuitBreakerException(String message, String code, Throwable cause) {
        super(message, code, cause);
    }
}
