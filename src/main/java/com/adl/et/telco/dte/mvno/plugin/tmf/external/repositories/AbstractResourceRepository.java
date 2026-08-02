package com.adl.et.telco.dte.mvno.plugin.tmf.external.repositories;

import com.adl.et.telco.dte.mvno.plugin.tmf.application.exception.type.FilterException;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.boundary.ResourceRepositoryInterface;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.*;
import com.adl.et.telco.dte.mvno.plugin.tmf.external.repositories.utils.MongoSequenceEntity;
import org.bson.Document;
import org.slf4j.MDC;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Base repository for Resource entities using MongoDB.
 *
 * @param <T> Resource entity.
 */
public abstract class AbstractResourceRepository<T extends BaseResourceDocument> implements ResourceRepositoryInterface<T> {

    public static final String SEQUENCE_COLLECTION = "idSequence";

    private static final long ID_BLOCK_SIZE = 100;

    private static final String FILTERS_FIELD = "filters";
    private static final String FILTER_ERROR_CODE = "INVALID_FILTER";
    private static final String COUNT_FIELD = "total";

    private static final Pattern INTEGER_VALUE = Pattern.compile("^[+-]?\\d+$");
    private static final Pattern DECIMAL_VALUE = Pattern.compile("^[+-]?\\d+\\.\\d+$");
    private static final Pattern DATE_VALUE = Pattern.compile("^\\d{4}-\\d{2}-\\d{2}$");
    private static final Pattern DATE_TIME_VALUE =
            Pattern.compile("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}(:\\d{2})?(\\.\\d+)?(Z|[+-]\\d{2}:?\\d{2})$");

    private final MongoTemplate mongoTemplate;
    private final Class<T> clazz;
    private final Executor queryExecutor;

    private final Object idBlockLock = new Object();
    private String idBlockPrefix;
    private long idBlockNext;
    private long idBlockLimit;

    /**
     * Constructor for Abstract repository.
     *
     * @param mongoTemplate MongoTemplate for handling DB operations.
     * @param clazz         Class of the resource entity.
     * @param queryExecutor Shared executor for parallel count+results queries.
     */
    protected AbstractResourceRepository(MongoTemplate mongoTemplate, Class<T> clazz, Executor queryExecutor) {
        this.mongoTemplate = mongoTemplate;
        this.clazz = clazz;
        this.queryExecutor = queryExecutor;
    }

    /**
     * Query resources in MongoDB.
     *
     * @param filters  Filters to apply.
     * @param fields   Comma separated fields to select.
     * @param pageable Pageable for pagination.
     * @return Page of matching resources.
     */
    @Override
    public Page<T> query(List<Filter> filters, String fields, Pageable pageable) {
        List<Criteria> allCriteria = new ArrayList<>();

        // -------------------------------
        // 1. Build match criteria
        // -------------------------------
        if (filters != null && !filters.isEmpty()) {
            List<Filter> filtersWithout = filters.stream()
                    .filter(rs -> !FILTERS_FIELD.equals(rs.getField()))
                    .collect(Collectors.toList());

            if (!filtersWithout.isEmpty()) {
                /*
                modifyFilters(filtersWithout);       ignore modify filters specific reason
                 */
                Optional<Criteria> baseCriteriaOpt = createMatchCriteria(filtersWithout);
                baseCriteriaOpt.ifPresent(allCriteria::add);
            }

            List<Criteria> extraCriteria = createFilterCriteria(filters);
            if (!extraCriteria.isEmpty()) {
                allCriteria.addAll(extraCriteria);
            }
        }

        Criteria finalCriteria = new Criteria();
        if (!allCriteria.isEmpty()) {
            finalCriteria.andOperator(allCriteria.toArray(new Criteria[0]));
        }

        // Custom aggregation stages contributed by the concrete repository.
        List<AggregationOperation> customOperations = new ArrayList<>();
        addOperations(customOperations);

        List<String> projectionFields = parseFields(fields);

        // -------------------------------
        // 2. Pagination path (aggregation)
        // -------------------------------
        if (pageable.isPagination()) {
            List<AggregationOperation> baseOps = new ArrayList<>();

            if (!allCriteria.isEmpty()) {
                baseOps.add(Aggregation.match(finalCriteria));
            }

            baseOps.addAll(customOperations);

            // Sorting (only for paged)
            baseOps.add(Aggregation.sort(Sort.Direction.DESC, "lastUpdate"));

            // Projection
            baseOps.add(createProjection(projectionFields));

            // Add skip/limit. Mongo rejects a non-positive $limit, so the stage is only added
            // when a limit was actually requested.
            if (pageable.getOffset() > 0) {
                baseOps.add(Aggregation.skip(pageable.getOffset()));
            }
            if (pageable.getLimit() > 0) {
                baseOps.add(Aggregation.limit(pageable.getLimit()));
            }

            Aggregation resultAgg = Aggregation.newAggregation(baseOps);

            // Run in parallel using shared executor (not per-request pool)
            CompletableFuture<List<T>> resultsFuture = CompletableFuture.supplyAsync(
                    onRequestContext(() -> mongoTemplate.aggregate(resultAgg, clazz, clazz).getMappedResults()),
                    queryExecutor);

            CompletableFuture<Long> countFuture = CompletableFuture.supplyAsync(
                    onRequestContext(() -> count(allCriteria.isEmpty() ? null : finalCriteria, customOperations)),
                    queryExecutor);

            List<T> results = resultsFuture.join();
            Long total = countFuture.join();

            return new Page<>(results, total);
        }

        // -------------------------------
        // 3. No pagination path
        // -------------------------------

        // Custom stages can only be honoured through the aggregation framework.
        if (!customOperations.isEmpty()) {
            List<AggregationOperation> ops = new ArrayList<>();

            if (!allCriteria.isEmpty()) {
                ops.add(Aggregation.match(finalCriteria));
            }

            ops.addAll(customOperations);
            ops.add(createProjection(projectionFields));

            List<T> aggregated = mongoTemplate.aggregate(Aggregation.newAggregation(ops), clazz, clazz)
                    .getMappedResults();

            return new Page<>(aggregated, aggregated.size());
        }

        Query query = new Query();
        if (!allCriteria.isEmpty()) {
            query.addCriteria(finalCriteria);
        }

        // Projection (include only requested fields)
        projectionFields.forEach(f -> query.fields().include(f));

        // No global sort here – avoids in-memory full collection sort
        // If business requires sort, ensure an index exists and add it here.

        List<T> results = mongoTemplate.find(query, clazz);

        // total == results.size() on this path — no skip/limit applied, so count() would be redundant
        return new Page<>(results, results.size());
    }

    /**
     * Carry the request context of the calling thread into a task that runs on the shared
     * executor. Per-request state - the tenant and catalog headers read through the request
     * scoped {@code HttpServletRequest}, and the logging MDC - is held in thread locals, so
     * without this a pool thread fails with "No thread-bound request found". The caller joins on
     * the futures before returning, so the request is still active while the task runs.
     *
     * @param task Task to run on the executor.
     * @param <R>  Result of the task.
     * @return Task wrapped with the context of the calling thread.
     */
    private <R> Supplier<R> onRequestContext(Supplier<R> task) {

        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return () -> {
            if (requestAttributes != null) {
                RequestContextHolder.setRequestAttributes(requestAttributes);
            }
            if (contextMap != null) {
                MDC.setContextMap(contextMap);
            }
            try {
                return task.get();
            } finally {
                RequestContextHolder.resetRequestAttributes();
                MDC.clear();
            }
        };
    }

    /**
     * Count matching documents. A plain count is used unless the concrete repository contributed
     * custom aggregation stages, in which case the count has to run through the same pipeline so
     * that the total stays consistent with the returned page.
     *
     * @param criteria         Match criteria, null when no filters were given.
     * @param customOperations Custom stages contributed via {@link #addOperations(List)}.
     * @return Total number of matching documents.
     */
    private long count(Criteria criteria, List<AggregationOperation> customOperations) {

        if (customOperations.isEmpty()) {
            Query countQuery = new Query();

            if (criteria != null) {
                countQuery.addCriteria(criteria);
            }

            return mongoTemplate.count(countQuery, clazz);
        }

        List<AggregationOperation> countOps = new ArrayList<>();

        if (criteria != null) {
            countOps.add(Aggregation.match(criteria));
        }

        countOps.addAll(customOperations);
        countOps.add(Aggregation.count().as(COUNT_FIELD));

        Document result = mongoTemplate.aggregate(Aggregation.newAggregation(countOps), clazz, Document.class)
                .getUniqueMappedResult();

        return result == null ? 0L : result.get(COUNT_FIELD, Number.class).longValue();
    }

    /**
     * Split the comma separated field selection, dropping blanks so that a trailing comma or a
     * space after a comma does not reach MongoDB as an empty field name.
     *
     * @param fields Comma separated fields to select.
     * @return Field names to project.
     */
    private List<String> parseFields(String fields) {

        if (fields == null || fields.isEmpty()) {
            return Collections.emptyList();
        }

        return Arrays.stream(fields.split(","))
                .map(String::trim)
                .filter(field -> !field.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Create the projection stage for the given field selection.
     *
     * @param projectionFields Fields to select, empty to project the full entity.
     * @return Projection operation.
     */
    private AggregationOperation createProjection(List<String> projectionFields) {

        return projectionFields.isEmpty()
                ? Aggregation.project(clazz)
                : Aggregation.project(projectionFields.toArray(new String[0]));
    }


    /**
     * Get resource by ID.
     *
     * @param id ID of the resource.
     * @return Optional found resource.
     */
    @Override
    public Optional<T> get(String id) {

        return Optional.ofNullable(mongoTemplate.findById(id, clazz));
    }

    /**
     * Save given resource in DB.
     *
     * @param entity Resource entity.
     * @return Saved resource.
     */
    @Override
    public T save(T entity) {

        if (entity.getId() == null) {
            entity.setId(retrieveNextId());
        }
        return mongoTemplate.save(entity);
    }

    /**
     * Delete resource using ID.
     *
     * @param id ID of the resource.
     */
    @Override
    public void delete(String id) {

        Query query = new Query().addCriteria(Criteria.where("id").is(id));

        mongoTemplate.remove(query, clazz);
    }

    /**
     * Check whether resource is existing by ID.
     *
     * @param id ID of the resource.
     * @return Boolean specifying existance.
     */
    @Override
    public boolean exists(String id) {

        Query query = new Query().addCriteria(Criteria.where("id").is(id));

        return mongoTemplate.exists(query, clazz);
    }

    /**
     * Implement to add custom MongoDB aggregation operations to the query pipeline. The added
     * operations run right after the filter match stage, before sorting and projection, and are
     * applied to the total count as well so that the count stays consistent with the page.
     *
     * @param operations List to add the custom aggregation operations to.
     */
    protected void addOperations(List<AggregationOperation> operations) {

    }

    /**
     * Implement to modify filters.
     *
     * @param filters Filters as received.
     */
    protected void modifyFilters(List<Filter> filters) {

    }

    /**
     * Add matching criteria if filters are present.
     *
     * @param filters Received filters list.
     * @return Criteria for matching.
     */
    private Optional<Criteria> createMatchCriteria(List<Filter> filters) {
        List<Criteria> criteriaList = new ArrayList<>();

        for (Filter filter : filters) {
            if (filter.getValue() == null || filter.getValue().length == 0 || filter.getOperation() == null) {
                continue;
            }

            String field = filter.getField();
            String firstValue = filter.getValue()[0].toString();

            switch (filter.getOperation()) {
                case IS:
                    criteriaList.add(createEqualityCriteria(field, filter.getValue()));
                    break;

                case GT:
                    criteriaList.add(Criteria.where(field).gt(parseValue(firstValue)));
                    break;

                case GTE:
                    criteriaList.add(Criteria.where(field).gte(parseValue(firstValue)));
                    break;

                case LT:
                    criteriaList.add(Criteria.where(field).lt(parseValue(firstValue)));
                    break;

                case LTE:
                    criteriaList.add(Criteria.where(field).lte(parseValue(firstValue)));
                    break;

                case REGEX:
                    criteriaList.add(Criteria.where(field).regex(compilePattern(firstValue, true)));
                    break;

                default:
                    // Unsupported ops: skip silently or throw exception
                    break;
            }
        }

        return criteriaList.isEmpty()
                ? Optional.empty()
                : Optional.of(new Criteria().andOperator(criteriaList.toArray(new Criteria[0])));
    }

    /**
     * Create equality criteria for a filter. Query parameters always arrive as strings, so the
     * typed representation of each value is matched as well — otherwise equality filters on
     * numeric, boolean or date fields never match anything.
     *
     * @param field  Field to match.
     * @param values Received values.
     * @return Equality criteria.
     */
    private Criteria createEqualityCriteria(String field, Object[] values) {

        List<Object> candidates = new ArrayList<>();

        for (Object value : values) {
            if (value == null) {
                continue;
            }

            String raw = value.toString();

            if (!candidates.contains(raw)) {
                candidates.add(raw);
            }

            Object parsed = parseValue(raw);

            if (!candidates.contains(parsed)) {
                candidates.add(parsed);
            }
        }

        if (candidates.isEmpty()) {
            return new Criteria();
        }

        return candidates.size() == 1
                ? Criteria.where(field).is(candidates.get(0))
                : Criteria.where(field).in(candidates);
    }

    /**
     * Compile a client supplied regular expression.
     *
     * @param regex           Regular expression as received.
     * @param caseInsensitive Whether matching should ignore case.
     * @return Compiled pattern.
     */
    private static Pattern compilePattern(String regex, boolean caseInsensitive) {

        try {
            return caseInsensitive ? Pattern.compile(regex, Pattern.CASE_INSENSITIVE) : Pattern.compile(regex);
        } catch (PatternSyntaxException e) {
            throw new FilterException("Invalid regular expression in filter: " + regex, FILTER_ERROR_CODE);
        }
    }

    public String retrieveNextId() {

        synchronized (idBlockLock) {
            if (idBlockNext >= idBlockLimit && !allocateIdBlock()) {
                return null;
            }

            String id = idBlockPrefix + idBlockNext;
            idBlockNext++;
            return id;
        }
    }

    /**
     * Reserve the next block of {@value #ID_BLOCK_SIZE} IDs from the shared sequence document
     * in one round trip, so concurrent create() calls only contend on Mongo once per block
     * instead of once per call.
     *
     * @return true if a block was reserved, false if the sequence document does not exist.
     */
    private boolean allocateIdBlock() {

        Update update = new Update();
        update.inc("current", ID_BLOCK_SIZE);

        MongoSequenceEntity sequenceEntity = mongoTemplate.findAndModify(
                new Query().addCriteria(Criteria.where("name").is(clazz.getSimpleName())),
                update,
                MongoSequenceEntity.class, SEQUENCE_COLLECTION);

        if (sequenceEntity == null) {
            return false;
        }

        idBlockPrefix = sequenceEntity.getPrefix();
        idBlockNext = sequenceEntity.getCurrent();
        idBlockLimit = idBlockNext + ID_BLOCK_SIZE;
        return true;
    }


    private List<Criteria> createFilterCriteria(List<Filter> filters) {
        if (filters == null) {
            return Collections.emptyList();
        }

        List<Criteria> criteriaList = new ArrayList<>();

        for (Filter rs : filters) {
            if (!FILTERS_FIELD.equals(rs.getField()) || rs.getValue() == null) {
                continue;
            }

            for (Object element : rs.getValue()) {
                if (element == null || element.toString().trim().isEmpty()) {
                    continue;
                }

                String expr = element.toString().trim();

                if (expr.contains("[?(")) {
                    // Array field with condition
                    int conditionStart = expr.indexOf("[?(");
                    int conditionEnd = expr.indexOf(")]", conditionStart);

                    if (conditionEnd < 0) {
                        throw new FilterException("Unterminated array filter expression: " + expr,
                                FILTER_ERROR_CODE);
                    }

                    String arrayField = expr.substring(0, conditionStart).trim();
                    String conditionPart = expr.substring(conditionStart + 3, conditionEnd);

                    if (arrayField.isEmpty()) {
                        throw new FilterException("Missing array field in filter expression: " + expr,
                                FILTER_ERROR_CODE);
                    }

                    Criteria elemMatch = parseLogicalExpression(conditionPart);
                    criteriaList.add(Criteria.where(arrayField).elemMatch(elemMatch));

                } else {
                    // Top-level OR/AND logical expression
                    Criteria topCriteria = parseLogicalExpression(expr);
                    criteriaList.add(topCriteria);
                }
            }
        }

        return criteriaList;
    }

    /**
     * Parse a logical filter expression. {@code ||} binds looser than {@code &&}, so the
     * expression is split on {@code ||} first and each part is then parsed on its own.
     *
     * @param expression Logical expression.
     * @return Criteria for the expression.
     */
    private Criteria parseLogicalExpression(String expression) {

        String[] orParts = expression.split("\\|\\|");
        if (orParts.length > 1) {
            List<Criteria> orCriteriaList = Arrays.stream(orParts)
                    .map(this::parseLogicalExpression)
                    .collect(Collectors.toList());
            return new Criteria().orOperator(orCriteriaList.toArray(new Criteria[0]));
        }

        String[] andParts = expression.split("&&");
        if (andParts.length > 1) {
            List<Criteria> andCriteriaList = Arrays.stream(andParts)
                    .map(this::parseSimpleConditions)
                    .collect(Collectors.toList());
            return new Criteria().andOperator(andCriteriaList.toArray(new Criteria[0]));
        }

        return parseSimpleConditions(expression);
    }

    /**
     * Parse a single {@code field==value} style condition.
     *
     * @param rawCondition Condition to parse.
     * @return Criteria for the condition.
     */
    private Criteria parseSimpleConditions(String rawCondition) {
        String condition = rawCondition.trim().replaceAll("@\\.", "");

        if (condition.contains("regex==")) {
            String[] parts = condition.split("regex==", 2);
            String field = parts[0].trim();
            String regex = parts[1].replace("'", "").trim();

            if (!field.isEmpty() && !regex.isEmpty()) {
                // Kept case sensitive to preserve the behaviour of existing filter expressions.
                return Criteria.where(field).regex(compilePattern(regex, false));
            }
        } else if (condition.contains("!=")) {
            String[] parts = condition.split("!=", 2);
            String field = parts[0].trim();
            String val = parts[1].replace("'", "").trim();

            if (!field.isEmpty()) {
                return Criteria.where(field).ne(val);
            }
        } else if (condition.contains("==")) {
            String[] parts = condition.split("==", 2);
            String field = parts[0].trim();
            String val = parts[1].replace("'", "").trim();

            if (!field.isEmpty()) {
                return Criteria.where(field).is(val);
            }
        }

        // An unparseable condition used to fall back to empty criteria, which silently matched
        // every document in the collection. Reject it instead.
        throw new FilterException("Unsupported filter expression: " + rawCondition.trim(), FILTER_ERROR_CODE);
    }

    /**
     * Convert a received filter value into the type it should be matched as. Values arrive as
     * strings, optionally single quoted, and are matched as string when no other type applies.
     *
     * @param rawValue Value as received.
     * @return Typed value to match on.
     */
    private static Object parseValue(String rawValue) {

        String value = rawValue.replace("'", "").trim();

        if (INTEGER_VALUE.matcher(value).matches()) {
            try {
                return Integer.valueOf(value);
            } catch (NumberFormatException e) {
                // Larger than an int, e.g. an epoch milliseconds timestamp.
                try {
                    return Long.valueOf(value);
                } catch (NumberFormatException ex) {
                    return value;
                }
            }
        }

        if (DECIMAL_VALUE.matcher(value).matches()) {
            return Double.valueOf(value);
        }

        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.valueOf(value);
        }

        // Date format 'yyyy-MM-dd', with or without the surrounding quotes.
        if (DATE_VALUE.matcher(value).matches()) {
            try {
                return LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE)
                        .atStartOfDay().atOffset(ZoneOffset.UTC);
            } catch (DateTimeParseException e) {
                throw new FilterException("Invalid date in filter: " + rawValue, FILTER_ERROR_CODE);
            }
        }

        // Offset date time format 'yyyy-MM-ddTHH:mm:ss±HH:mm', 'Z' included.
        if (DATE_TIME_VALUE.matcher(value).matches()) {
            try {
                return OffsetDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            } catch (DateTimeParseException e) {
                throw new FilterException("Invalid date time in filter: " + rawValue, FILTER_ERROR_CODE);
            }
        }

        return value;
    }
}
