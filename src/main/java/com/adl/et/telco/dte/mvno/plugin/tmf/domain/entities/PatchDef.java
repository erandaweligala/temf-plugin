package com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities;

import lombok.Data;

@Data
public class PatchDef {

    private String op;
    private String path;
    private Object value;
}
