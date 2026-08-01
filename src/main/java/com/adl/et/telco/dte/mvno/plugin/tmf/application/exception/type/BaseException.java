/*
  Copyrights 2020 Axiata Digital Labs Pvt Ltd.
  All Rights Reserved.
  <p>
  These material are unpublished, proprietary, confidential source
  code of Axiata Digital Labs Pvt Ltd (ADL) and constitute a TRADE
  SECRET of ADL.
  <p>
  ADL retains all title to and intellectual property rights in these
  materials.
 */
package com.adl.et.telco.dte.mvno.plugin.tmf.application.exception.type;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class BaseException extends RuntimeException {
    private final String code;

    public String getCode() {
        return code;
    }

    public BaseException(String message) {

        super(message);
        code = "";
    }

    public BaseException(String message, String code) {

        super(message);
        this.code = code;
    }

    public BaseException(String message, String code, Throwable cause) {

        super(message, cause);
        this.code = code;
    }
}
