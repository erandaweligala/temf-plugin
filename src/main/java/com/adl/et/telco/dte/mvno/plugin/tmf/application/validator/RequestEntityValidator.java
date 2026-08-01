/*
  Copyrights 2020 Axiata Digital Labs Pvt Ltd.
  All Rights Reserved.

  These material are unpublished, proprietary, confidential source
  code of Axiata Digital Labs Pvt Ltd (ADL) and constitute a TRADE
  SECRET of ADL.

  ADL retains all title to and intellectual property rights in these
  materials.

 */
package com.adl.et.telco.dte.mvno.plugin.tmf.application.validator;

import com.adl.et.telco.dte.mvno.plugin.tmf.application.exception.type.ValidationException;
import org.springframework.stereotype.Component;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.List;
import java.util.Set;

@Component
public class RequestEntityValidator {

    private final Validator validator;

    public RequestEntityValidator(Validator validator) {
        this.validator = validator;
    }

    public <T> void validate(T target) throws ValidationException {

        Set<ConstraintViolation<T>> errors = this.validator.validate(target);

        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }
    }

    public <T> void validateList(List<T> target) {

        if (target != null) {
            target.forEach(this::validate);
        }
    }
}