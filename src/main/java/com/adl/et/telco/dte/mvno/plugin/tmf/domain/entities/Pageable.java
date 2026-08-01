package com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities;

import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@EqualsAndHashCode
public class Pageable {

    private final long offset;
    private final int limit;
    private final boolean pagination;

    public Pageable(long offset, int limit) {

        this.offset = offset;
        this.limit = limit;
        pagination = true;
    }

    public Pageable() {

        this.offset = 0;
        this.limit = 0;
        this.pagination = false;
    }
}
