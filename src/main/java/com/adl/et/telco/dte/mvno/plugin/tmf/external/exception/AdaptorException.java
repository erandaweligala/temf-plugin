/*
  Copyrights 2020 Axiata Digital Labs Pvt Ltd.
  All Rights Reserved.

  These material are unpublished, proprietary, confidential source
  code of Axiata Digital Labs Pvt Ltd (ADL) and constitute a TRADE
  SECRET of ADL.

  ADL retains all title to and intellectual property rights in these
  materials.

 */
package com.adl.et.telco.dte.mvno.plugin.tmf.external.exception;

import com.adl.et.telco.dte.mvno.plugin.tmf.application.exception.type.BaseException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class AdaptorException extends BaseException {

    public AdaptorException(String message) {
        super(message);
    }

    public AdaptorException(String message, String code) {
        super(message, code);
    }

    public AdaptorException(String message, String code, Throwable cause) {
        super(message, code, cause);
    }
}
