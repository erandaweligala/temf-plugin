package com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities;

import lombok.Getter;

import java.util.List;

@Getter
public class Page<T> {

    private final List<T> content;
    private final long total;

    public Page(List<T> content, long total) {
        this.content = content;
        this.total = total;
    }
}
