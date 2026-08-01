package com.adl.et.telco.dte.mvno.plugin.tmf.external.repositories.utils;

import lombok.Data;

@Data
public class MongoSequenceEntity {

    private String id;
    private String name;
    private long start;
    private long end;
    private String prefix;
    private long current;
}
