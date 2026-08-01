package com.adl.et.telco.dte.mvno.plugin.tmf.application.transport.request.entities;

import lombok.Data;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;

@Data
public class PatchDefRequestEntity {

    @NotEmpty
    @Pattern(regexp = "(add|remove|replace)", message = "operation must be of add, remove or replace")
    private String op;
    @NotEmpty
    private String path;
    private Object value;
}
