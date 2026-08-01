package com.adl.et.telco.dte.mvno.plugin.tmf.domain.boundary;

import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.BaseResourceDocument;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.Filter;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.Page;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.Pageable;

import java.util.List;
import java.util.Optional;

public interface ResourceRepositoryInterface<T extends BaseResourceDocument> {

    Page<T> query(List<Filter> filters, String fields, Pageable pageable);

    Optional<T> get(String id);

    T save(T entity);

    void delete(String id);

    boolean exists(String id);
}
