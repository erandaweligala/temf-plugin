package com.adl.et.telco.dte.mvno.plugin.tmf.application.controller;

import com.adl.et.telco.dte.mvno.plugin.tmf.application.exception.type.BaseException;
import com.adl.et.telco.dte.mvno.plugin.tmf.application.exception.type.CircuitBreakerException;
import com.adl.et.telco.dte.mvno.plugin.tmf.application.exception.type.FilterException;
import com.adl.et.telco.dte.mvno.plugin.tmf.application.exception.type.ServerException;
import com.adl.et.telco.dte.mvno.plugin.tmf.application.exception.type.ValidationException;
import com.adl.et.telco.dte.mvno.plugin.tmf.application.transport.request.entities.PatchDefRequestEntity;
import com.adl.et.telco.dte.mvno.plugin.tmf.application.transport.response.transformers.ResponseTransformer;
import com.adl.et.telco.dte.mvno.plugin.tmf.application.validator.RequestEntityValidator;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.*;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.service.BaseResourceService;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.Explode;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.enums.ParameterStyle;
import io.swagger.v3.oas.annotations.media.Schema;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Base controller for managing resources.
 *
 * @param <T> Entity of the resource.
 * @param <C> Create entity of the resource.
 * @param <U> Update entity of the resource.
 */
public abstract class BaseResourceController<T extends BaseResourceDocument, C, U> {

    private static final String RESULT_COUNT_HEADER = "X-Result-Count";
    private static final String TOTAL_COUNT_HEADER = "X-Total-Count";
    private static final String PAGINATION_ERROR_CODE = "INVALID_PAGINATION";

    private final BaseResourceService<T> service;
    private final ResponseTransformer<T> transformer;
    private final RequestEntityValidator validator;
    private final ObjectMapper objectMapper;
    private final Class<T> clazz;
    private final Class<U> updateClazz;

    /**
     * Base Controller Constructor.
     *
     * @param service     Service layer component for managing resource.
     * @param transformer Transformer for transforming response using resource entity.
     * @param validator   Validator.
     * @param clazz       Class of the resource entity.
     * @param updateClazz Class of the update request entity.
     */
    protected BaseResourceController(BaseResourceService<T> service,
                                     ResponseTransformer<T> transformer,
                                     RequestEntityValidator validator,
                                     Class<T> clazz,
                                     Class<U> updateClazz) {

        this.service = service;
        this.transformer = transformer;
        this.validator = validator;
        this.clazz = clazz;
        this.updateClazz = updateClazz;

        this.objectMapper = new ObjectMapper().setDefaultPropertyInclusion(JsonInclude.Include.NON_NULL)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .findAndRegisterModules();
    }

    /**
     * Query handling for the resource.
     *
     * @param filters Filters to apply on query.
     * @param fields  Comma separated top level fields to return.
     * @param offset  Offset of the pagination.
     * @param limit   Limit of the pagination.
     * @return Query result.
     */
    @Operation(summary = "Query resources",
            description = "Filters are sent as plain query parameters. `name=value` matches on "
                    + "equality, the `.eq`, `.gt`, `.gte`, `.lt`, `.lte` and `.regex` suffixes "
                    + "select the operation, comma separated values are matched with OR, and "
                    + "nested fields are addressed with a dot, for example "
                    + "`attachment.name=pic1,pic2`.",
            parameters = {
                    // The filters are a catch-all map, so there is no method argument to derive
                    // the query parameter names from. Documented as a free form object that is
                    // exploded into separate query parameters, which is how it arrives here.
                    @Parameter(name = "filters", in = ParameterIn.QUERY,
                            description = "Field name and value pairs to filter on.",
                            style = ParameterStyle.FORM, explode = Explode.TRUE,
                            example = "{\"name\": \"data plan\", \"lastUpdate.gte\": \"2024-01-01\"}",
                            schema = @Schema(type = "object",
                                    additionalProperties = Schema.AdditionalPropertiesValue.TRUE)),
                    @Parameter(name = "fields", in = ParameterIn.QUERY,
                            description = "Comma separated top level fields to return.",
                            example = "name,description"),
                    @Parameter(name = "offset", in = ParameterIn.QUERY,
                            description = "Number of resources to skip.", example = "0"),
                    @Parameter(name = "limit", in = ParameterIn.QUERY,
                            description = "Maximum number of resources to return.", example = "10")
            })
    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    @CircuitBreaker(name = "base_query", fallbackMethod = "circuitBreakerFallback")
    public ResponseEntity<List<Map<String, Object>>> query(
            @RequestParam(required = false) Map<String, String> filters,
            @RequestParam(required = false) String fields,
            @RequestParam(required = false) String offset,
            @RequestParam(required = false) String limit,
            HttpServletRequest request) {

        String processedFields = preProcessFields(fields);

        // Service layer call.
        Page<T> result = service.query(createFilter(filters), processedFields, createPageable(offset, limit));

        // Get transformer according to fields.
        Function<T, Map<String, Object>> mapFunction = (processedFields == null) ? transformer::transform :
                category -> transformer.transform(category, Arrays.asList(processedFields.split(",")));

        List<Map<String, Object>> transformed = result.getContent().stream()
                .map(mapFunction)
                .collect(Collectors.toList());

        // Response.
        return ResponseEntity.ok()
                .header(RESULT_COUNT_HEADER, "" + result.getContent().size())
                .header(TOTAL_COUNT_HEADER, "" + result.getTotal())
                .body(transformed);
    }

    /**
     * Get resource by ID.
     *
     * @param id     ID of the resource.
     * @param fields Top level fields to return.
     * @return Resource by ID.
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @CircuitBreaker(name = "base_get", fallbackMethod = "circuitBreakerFallback")
    public ResponseEntity<Map<String, Object>> get(@PathVariable String id,
                                                   @RequestParam(required = false) String fields,
                                                   HttpServletRequest request) {

        String processedFields = preProcessFields(fields);

        Map<String, Object> transformed = (processedFields == null) ? transformer.transform(service.get(id)) :
                transformer.transform(service.get(id), Arrays.asList(processedFields.split(",")));

        return ResponseEntity.ok(transformed);
    }

    /**
     * Creating new resource.
     *
     * @param requestEntity Request entity of the new resource.
     * @return Created resource.
     */
    @PostMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    @CircuitBreaker(name = "base_create", fallbackMethod = "circuitBreakerFallback")
    public ResponseEntity<Map<String, Object>> create(@RequestBody C requestEntity, HttpServletRequest request) {

        validator.validate(requestEntity);

        T entity = objectMapper.convertValue(requestEntity, clazz);

        entity = service.create(entity);

        return ResponseEntity.status(HttpStatus.CREATED).body(transformer.transform(entity));
    }

    /**
     * Patch resource using json-merge.
     *
     * @param id            ID of the resource.
     * @param requestEntity Patch request.
     * @return Patched resource.
     */
    @PatchMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = "application/merge-patch+json")
    @CircuitBreaker(name = "base_patch", fallbackMethod = "circuitBreakerFallback")
    public ResponseEntity<Map<String, Object>> mergeUpdate(
            @RequestBody Map<String, Object> requestEntity, @PathVariable String id, HttpServletRequest request) {

        U updateEntity = objectMapper.convertValue(requestEntity, updateClazz);

        validator.validate(updateEntity);

        T entity = service.update(id, requestEntity);

        return ResponseEntity.ok(transformer.transform(entity));
    }

    /**
     * Patch resource using json-patch.
     *
     * @param id            ID of the resource.
     * @param requestEntity Patch request.
     * @return Patched resource.
     */
    @PatchMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE, consumes = "application/json-patch+json")
    @CircuitBreaker(name = "base_patch", fallbackMethod = "circuitBreakerFallback")
    public ResponseEntity<Map<String, Object>> patchUpdate(
            @RequestBody List<PatchDefRequestEntity> requestEntity, @PathVariable String id, HttpServletRequest request) {

        validator.validateList(requestEntity);

        List<PatchDef> patchDefs = objectMapper.convertValue(requestEntity, objectMapper.getTypeFactory()
                .constructCollectionType(List.class, PatchDef.class));

        T entity = service.update(id, patchDefs);

        return ResponseEntity.ok(transformer.transform(entity));
    }

    /**
     * Delete resource using ID of the resource.
     *
     * @param id ID of the resource.
     * @return Response with no content.
     */
    @DeleteMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @CircuitBreaker(name = "base_delete", fallbackMethod = "circuitBreakerFallback")
    public ResponseEntity<Void> delete(@PathVariable String id, HttpServletRequest request) {


        service.delete(id);

        return ResponseEntity.noContent().build();
    }

    /**
     * Creating pageable using limit and offset. Either parameter may be given on its own; a
     * missing offset starts at the beginning and a missing limit returns the remaining resources.
     *
     * @param offsetString pagination offset.
     * @param limitString  limit offset.
     * @return Pageable object.
     */
    private Pageable createPageable(String offsetString, String limitString) {

        // Check case where no pagination params are defined and create pagination disabled Pageable.
        if (offsetString == null && limitString == null) {
            return new Pageable();
        }

        long offset = parsePaginationParam("offset", offsetString);
        long limit = parsePaginationParam("limit", limitString);

        return new Pageable(offset, (int) Math.min(limit, Integer.MAX_VALUE));
    }

    /**
     * Parse a pagination query parameter.
     *
     * @param name  Name of the parameter, for error reporting.
     * @param value Received value.
     * @return Parsed value, 0 when the parameter was not given.
     */
    private long parsePaginationParam(String name, String value) {

        if (value == null || value.trim().isEmpty()) {
            return 0;
        }

        long parsed;

        try {
            parsed = Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            throw new FilterException("Query parameter '" + name + "' must be a number but was: " + value,
                    PAGINATION_ERROR_CODE);
        }

        if (parsed < 0) {
            throw new FilterException("Query parameter '" + name + "' must not be negative", PAGINATION_ERROR_CODE);
        }

        return parsed;
    }

    /**
     * Preprocess fields to return.
     *
     * @param fields Received comma separated fields.
     * @return Processed fields.
     */
    private String preProcessFields(String fields) {

        if (fields == null) {
            return null;
        }

        // Remove lower level field selection
        String processed = Arrays.stream(fields.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty() && !s.contains("."))
                .reduce((s, s2) -> s.concat(",").concat(s2))
                .orElse(null);

        return (processed == null || processed.isEmpty()) ? null : processed;
    }

    /**
     * Create filter using received query params.
     *
     * @param filterMap Query parameter map.
     * @return Processed Filter list
     */
    private List<Filter> createFilter(Map<String, String> filterMap) {

        List<Filter> filters = new ArrayList<>();

        // Remove operational query params.
        filterMap.remove("fields");
        filterMap.remove("offset");
        filterMap.remove("limit");

        // Handle filters where for all ops.
        filterMap.forEach((key, val) -> {
            if (val != null && !val.isEmpty()) {
                if (key.endsWith(".gt")) {
                    filters.add(new Filter(key.substring(0, key.length() - 3), val.split(","), FilterOperation.GT));
                } else if (key.endsWith(".gte")) {
                    filters.add(new Filter(key.substring(0, key.length() - 4), val.split(","), FilterOperation.GTE));
                } else if (key.endsWith(".lt")) {
                    filters.add(new Filter(key.substring(0, key.length() - 3), val.split(","), FilterOperation.LT));
                } else if (key.endsWith(".lte")) {
                    filters.add(new Filter(key.substring(0, key.length() - 4), val.split(","), FilterOperation.LTE));
                } else if (key.endsWith(".regex")) {
                    filters.add(new Filter(key.substring(0, key.length() - 6), val.split(","), FilterOperation.REGEX));
                } else if (key.endsWith(".eq")) {
                    filters.add(new Filter(key.substring(0, key.length() - 3), val.split(","), FilterOperation.IS));
                } else {
                    filters.add(new Filter(key, val.split(","), FilterOperation.IS));
                }
            }
        });

        return filters;
    }

    /**
     * Handle circuit breaker fallback.
     *
     * @param t   Received throwable from circuit breaker.
     * @param <F> Return type
     * @return Nothing will be returned.
     */
    public <F> ResponseEntity<F> circuitBreakerFallback(Throwable t) {

        // Handle case of Circuit breaker open state.
        if (t instanceof ExecutionException) {

            throw new CircuitBreakerException("Could not handle request", "CIRBR_GET", t);
        }

        // Don't wrap custom defined exceptions.
        if (t instanceof BaseException) {
            throw (BaseException) t;
        }

        if (t instanceof ValidationException) {
            throw (ValidationException) t;
        }

        throw new ServerException(t.getMessage(), "SERVER_ERROR", t);
    }
}
