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

import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import javax.validation.ConstraintViolation;
import java.util.*;

@ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
public class ValidationException extends RuntimeException {

    @Getter
    private final transient Map<String, List<String>> errors;

    public <T> ValidationException(Set<ConstraintViolation<T>> errors) {
        this.errors = formatValidationErrors(errors);
    }

    private <T> Map<String, List<String>> formatValidationErrors(Set<ConstraintViolation<T>> errors) {

        Map<String, List<String>> errDetails = new LinkedHashMap<>();

        errors.forEach(error -> {

            String key = error.getPropertyPath().toString();
            String val = error.getMessage();

            // when a validation error already exists for the field
            if (errDetails.containsKey(key)) {

                List<String> arrVal = errDetails.get(key);
                arrVal.add(val);

                errDetails.put(key, arrVal);

                return;
            }

            // when there are no validation errors for the field
            ArrayList<String> arr = new ArrayList<>();
            arr.add(val);

            errDetails.put(key, arr);
        });

        return errDetails;
    }
}
