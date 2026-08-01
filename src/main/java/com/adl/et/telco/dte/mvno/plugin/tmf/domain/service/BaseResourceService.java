package com.adl.et.telco.dte.mvno.plugin.tmf.domain.service;

import com.adl.et.telco.dte.mvno.plugin.tmf.domain.boundary.MergeAdaptorInterface;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.boundary.NotifierAdaptorInterface;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.boundary.ResourceRepositoryInterface;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.BaseResourceDocument;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.Filter;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.Page;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.Pageable;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.PatchDef;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.exception.DomainException;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.notification.ResourceNotificationCreator;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.processor.ResourceProcessor;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Base service layer for managing resources.
 *
 * @param <T> Resource entity to manage.
 */
public abstract class BaseResourceService<T extends BaseResourceDocument> {

    private static final String ERROR_CODE_NOT_FOUND = "_NOT_FOUND";
    private static final String ERROR_MSG_NOT_FOUND = " not found for id";
    public static final String TENANT_ID = "tenantId";
    public static final String CATALOG_ID = "catalogId";
    public static final String TRACE_ID = "traceId";

    private final ResourceRepositoryInterface<T> repository;
    private final String entityCode;
    private final MergeAdaptorInterface mergeAdaptor;
    private final ResourceProcessor<T> processor;
    private final Class<T> clazz;
    private final ResourceNotificationCreator<T> notificationCreator;
    private final NotifierAdaptorInterface notifier;
    private final HttpServletRequest httpServletRequest;


    /**
     * Base service constructor.
     *  @param repository          repository layer component for resource entity.
     * @param entityCode          Entity code for error messages.
     * @param mergeAdaptor        Merge adaptor for handing merge.
     * @param clazz               Class of the resource entity.
     * @param notificationCreator Notification creator for resource.
     * @param notifier            Notifier.
     */
    protected BaseResourceService(ResourceRepositoryInterface<T> repository,
                                  String entityCode,
                                  MergeAdaptorInterface mergeAdaptor,
                                  ResourceProcessor<T> processor,
                                  Class<T> clazz, ResourceNotificationCreator<T> notificationCreator,
                                  NotifierAdaptorInterface notifier, HttpServletRequest httpServletRequest) {

        this.repository = repository;
        this.entityCode = entityCode;
        this.mergeAdaptor = mergeAdaptor;
        this.processor = processor;
        this.clazz = clazz;
        this.notificationCreator = notificationCreator;
        this.notifier = notifier;
        this.httpServletRequest = httpServletRequest;
    }

    /**
     * Query resource.
     *
     * @param filters  Filters for applying in quert.
     * @param fields   Top level fields to select in return.
     * @param pageable Pageable object for pagination.
     * @return Page of query objects.
     */
    public Page<T> query(List<Filter> filters, String fields, Pageable pageable) {

        Page<T> queryResult = repository.query(filters, fields, pageable);

        queryResult.getContent().forEach(processor::postProcess);

        return queryResult;
    }

    /**
     * Get resource by ID.
     *
     * @param id ID of the resource.
     * @return Matching resource.
     */
    public T get(String id) {

        // Throw exception if no entity found for given ID.
        T entity = repository.get(id).orElseThrow(() ->
                new DomainException(entityCode + ERROR_MSG_NOT_FOUND, entityCode + ERROR_CODE_NOT_FOUND));

        // Call post process to add redundant data,
        processor.postProcess(entity);

        return entity;
    }

    /**
     * Create resource.
     *
     * @param entity Content of the resource.
     * @return Created resource.
     */
    public T create(T entity) {

        // Call preCreateProcess for custom validations and field changes.
        processor.preCreateProcess(entity);

        entity = repository.save(entity);

        processor.postProcess(entity);

        notificationCreator.createNotifications(entity)
                .forEach(notifier::notify);

        return entity;
    }

    /**
     * Update resource using merge.
     *
     * @param id        ID of the resource.
     * @param updateReq Update request as a Map.
     * @return Updated full resource entity.
     */
    public T update(String id, Map<String, Object> updateReq) {

        // Get current and throw exception if not found.
        T existing = repository.get(id)
                .orElseThrow(() -> new DomainException(entityCode + ERROR_MSG_NOT_FOUND,
                        entityCode + ERROR_CODE_NOT_FOUND));

        // Pre-merge process calling for validating merge.
        processor.preMergeProcess(updateReq, existing);

        T entity = mergeAdaptor.mergePatch(updateReq, existing, clazz);

        processor.preUpdateProcess(entity);

        T updated = repository.save(entity);

        processor.postProcess(updated);

        notificationCreator.updateNotifications(updateReq, existing, entity, updated)
                .forEach(notifier::notify);

        return updated;
    }

    /**
     * Update resource using patch.
     *
     * @param id        ID of the resource.
     * @param updateReq Update request as a Map.
     * @return Updated full resource entity.
     */
    public T update(String id, List<PatchDef> updateReq) {

        // Get current and throw exception if not found.
        T existing = repository.get(id)
                .orElseThrow(() -> new DomainException(entityCode + ERROR_MSG_NOT_FOUND,
                        entityCode + ERROR_CODE_NOT_FOUND));

        // Pre-merge process calling for validating merge.
        processor.preMergeProcess(updateReq, existing);

        T entity = mergeAdaptor.mergePatch(updateReq, existing, clazz);

        processor.preUpdateProcess(entity);

        T updated = repository.save(entity);

        processor.postProcess(updated);

        notificationCreator.updateNotifications(updateReq, existing, entity, updated)
                .forEach(notifier::notify);

        return updated;
    }

    /**
     * Delete resource using ID.
     *
     * @param id ID of the resource.
     */
    public void delete(String id) {

        // Check existence.
        T entity = repository.get(id).orElseThrow(() ->
                new DomainException(entityCode + ERROR_MSG_NOT_FOUND, entityCode + ERROR_CODE_NOT_FOUND));

        repository.delete(id);

        notificationCreator.deleteNotifications(entity)
                .forEach(notifier::notify);
    }

    public String getCollectionName() {
        String catalog = null;
        String tenant = null;
        StringBuilder sb = new StringBuilder();
        if (Objects.nonNull(httpServletRequest.getHeader(CATALOG_ID)) && !httpServletRequest.getHeader(CATALOG_ID).equals("") ){
           // catalog = ;
            sb.append(httpServletRequest.getHeader(CATALOG_ID)).append("_");
        }
        if(Objects.nonNull(httpServletRequest.getHeader(TENANT_ID)) && !"".equals(httpServletRequest.getHeader(CATALOG_ID))){
            sb.append(httpServletRequest.getHeader(TENANT_ID)).append("_");
        }

        if(sb.length() == 0){
            return null;
        }
        return sb.toString();
    }

}
