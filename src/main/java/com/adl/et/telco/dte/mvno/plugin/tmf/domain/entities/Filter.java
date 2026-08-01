package com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Filter {

    private String field;
    private Object[] value;
    private FilterOperation operation;
}
