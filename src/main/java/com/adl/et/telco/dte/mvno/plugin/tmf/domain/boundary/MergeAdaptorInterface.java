package com.adl.et.telco.dte.mvno.plugin.tmf.domain.boundary;

import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.PatchDef;

import java.util.List;
import java.util.Map;

public interface MergeAdaptorInterface {

    <T> T mergePatch(Map<String, Object> update, T targetBean, Class<T> beanClass);

    <T> T mergePatch(List<PatchDef> update, T targetBean, Class<T> beanClass);
}
