package com.adl.et.telco.dte.mvno.plugin.tmf.domain.processor;

import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.BaseResourceDocument;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.PatchDef;

import java.util.List;
import java.util.Map;

public interface ResourceProcessor<T extends BaseResourceDocument> {

    /**
     * Will be called before creation of new resource.
     * Additional validations and changes can be done for resource entity.
     *
     * @param entity Saving entity.
     */
    void preCreateProcess(T entity);

    /**
     * Will be called before merging of new patch to existing resource.
     * Additional validations and changes can be done comparing changes to existing resource.
     *
     * @param updateReq Patch for resource.
     * @param entity  Existing resource entity.
     */
    void preMergeProcess(Map<String, Object> updateReq, T entity);

    void preMergeProcess(List<PatchDef> updateReq, T entity);

    /**
     * Will be called before updating of a resource.
     * Additional validations and changes can be done for resource entity.
     *
     * @param entity Updating entity.
     */
    void preUpdateProcess(T entity);

    /**
     * Will be called after repository operation and before returning resource entity.
     * Additional redundant fields can be set here.
     *
     * @param entity Saved or retrieved entity.
     */
    void postProcess(T entity);
}
