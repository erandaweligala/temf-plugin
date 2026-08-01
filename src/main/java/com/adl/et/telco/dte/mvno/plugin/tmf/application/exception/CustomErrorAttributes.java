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
package com.adl.et.telco.dte.mvno.plugin.tmf.application.exception;

import com.adl.et.telco.dte.mvno.plugin.tmf.application.alarm.AlarmGen;
import com.adl.et.telco.dte.mvno.plugin.tmf.application.exception.type.BaseException;
import com.adl.et.telco.dte.mvno.plugin.tmf.application.exception.type.ValidationException;
import com.adl.et.telco.dte.mvno.plugin.tmf.external.exception.WebClientException;
import com.adl.et.telco.dte.plugin.alarming.dto.AlarmDef;
import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.servlet.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;


@Component
public class CustomErrorAttributes extends DefaultErrorAttributes {

    private static final String MESSAGE_FIELD = "message";
    private static final String CODE_FIELD = "code";
    private static final String TYPE_FIELD = "type";

    private final AlarmGen alarm;

    public CustomErrorAttributes(AlarmGen alarm) {
        this.alarm = alarm;
    }

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Throwable error = getError(webRequest);
        if (error == null) {
            return null;
        }
        if (error instanceof ValidationException) {
            return handleValidationException((ValidationException) error);
        } else if (error instanceof BaseException) {
            return handleRecoverableException((BaseException) error,
                    options.isIncluded(ErrorAttributeOptions.Include.STACK_TRACE));
        }
        return handleGenericException(error, options.isIncluded(ErrorAttributeOptions.Include.STACK_TRACE));
    }

    /**
     * Handle validation exceptions
     *
     * @param error exception
     * @return error description
     */
    private Map<String, Object> handleValidationException(ValidationException error) {

        Map<String, Object> errorDetails = new LinkedHashMap<>();

        errorDetails.put(CODE_FIELD, "400");
        errorDetails.put(TYPE_FIELD, error.getClass().getSimpleName());
        errorDetails.put(MESSAGE_FIELD, error.getErrors());

        alarm.alert(AlarmDef.MessageType.FUNCTIONAL, error.getMessage());
        return errorDetails;
    }

    /**
     * Handle unrecoverable and more generic exceptions
     *
     * @param error exception
     * @return error description
     */
    private Map<String, Object> handleGenericException(Throwable error, boolean includeStackTrace) {

        Map<String, Object> errorDetails = new LinkedHashMap<>();

        errorDetails.put(CODE_FIELD, "500");
        errorDetails.put(TYPE_FIELD, error.getClass().getSimpleName());
        errorDetails.put(MESSAGE_FIELD, error.getMessage());

        if (includeStackTrace) {
            errorDetails.put("trace", this.getStackTrace(error));
        }

        if (error instanceof WebClientException) {
            alarm.alert(AlarmDef.MessageType.API, error.getMessage());
        } else {
            alarm.alert(AlarmDef.MessageType.FUNCTIONAL, error.getMessage());
        }
        return errorDetails;
    }

    /**
     * Handle recoverable exceptions
     *
     * @param error exception
     * @return error description
     */
    private Map<String, Object> handleRecoverableException(BaseException error,
                                                           boolean includeStackTrace) {

        Map<String, Object> errorDetails = new LinkedHashMap<>();

        errorDetails.put(CODE_FIELD, error.getCode() != null ? error.getCode() : "400");
        errorDetails.put(TYPE_FIELD, error.getClass().getSimpleName());
        errorDetails.put(MESSAGE_FIELD, error.getMessage());

        if (includeStackTrace) {
            errorDetails.put("trace", this.getStackTrace(error));
        }

        alarm.alert(AlarmDef.MessageType.FUNCTIONAL, error.getMessage());
        return errorDetails;
    }


    /**
     * Get stack trace from an exception
     *
     * @param error exception
     * @return stack trace
     */
    private String[] getStackTrace(Throwable error) {

        StringWriter stackTrace = new StringWriter();
        error.printStackTrace(new PrintWriter(stackTrace));
        stackTrace.flush();

        return stackTrace.toString().replace("\t", "    ").split("\r\n");
    }
}
