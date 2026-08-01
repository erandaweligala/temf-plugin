package com.adl.et.telco.dte.mvno.plugin.tmf.external.repositories;

import com.adl.et.telco.dte.mvno.plugin.tmf.domain.boundary.ResourceRepositoryInterface;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.*;
import com.adl.et.telco.dte.mvno.plugin.tmf.external.repositories.utils.MongoSequenceEntity;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.FacetOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Base repository for Resource entities using MongoDB.
 *
 * @param <T> Resource entity.
 */
public abstract class AbstractResourceRepository<T extends BaseResourceDocument> implements ResourceRepositoryInterface<T> {

    public static final String SEQUENCE_COLLECTION = "idSequence";

    private static final long ID_BLOCK_SIZE = 100;

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
                    .filter(rs -> !"filters".equals(rs.getField()))
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

        // -------------------------------
        // 2. Pagination path (aggregation)
        // -------------------------------
        if (pageable.isPagination()) {
            List<AggregationOperation> baseOps = new ArrayList<>();

            if (!allCriteria.isEmpty()) {
                baseOps.add(Aggregation.match(finalCriteria));
            }

            // Sorting (only for paged)
            baseOps.add(Aggregation.sort(Sort.Direction.DESC, "lastUpdate"));

            // Projection
            if (fields != null && !fields.isEmpty()) {
                baseOps.add(Aggregation.project(fields.split(",")));
            } else {
                baseOps.add(Aggregation.project(clazz));
            }

            // Add skip/limit
            baseOps.add(Aggregation.skip(pageable.getOffset()));
            baseOps.add(Aggregation.limit(pageable.getLimit()));

            Aggregation resultAgg = Aggregation.newAggregation(baseOps);

            // Count query (simple, not aggregation)
            Query countQuery = new Query();
            if (!allCriteria.isEmpty()) {
                countQuery.addCriteria(finalCriteria);
            }

            // Run in parallel using shared executor (not per-request pool)
            CompletableFuture<List<T>> resultsFuture = CompletableFuture.supplyAsync(
                    () -> mongoTemplate.aggregate(resultAgg, clazz, clazz).getMappedResults(), queryExecutor);

            CompletableFuture<Long> countFuture = CompletableFuture.supplyAsync(
                    () -> mongoTemplate.count(countQuery, clazz), queryExecutor);

            List<T> results = resultsFuture.join();
            Long total = countFuture.join();

            return new Page<>(results, total);
        }

        // -------------------------------
        // 3. No pagination path (direct find)
        // -------------------------------
        Query query = new Query();
        if (!allCriteria.isEmpty()) {
            query.addCriteria(finalCriteria);
        }

        // Projection (include only requested fields)
        if (fields != null && !fields.isEmpty()) {
            Arrays.stream(fields.split(",")).forEach(f -> query.fields().include(f));
        }

        // No global sort here – avoids in-memory full collection sort
        // If business requires sort, ensure an index exists and add it here.

        List<T> results = mongoTemplate.find(query, clazz);

        // total == results.size() on this path — no skip/limit applied, so count() would be redundant
        return new Page<>(results, results.size());
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
     * Implement to modify the aggregation operations.
     *
     * @param operations List of default MongoDB aggregation operations.
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
            if (filter.getValue() == null || filter.getValue().length == 0) {
                continue;
            }

            String field = filter.getField();
            String firstValue = filter.getValue()[0].toString();

            switch (filter.getOperation()) {
                case IS:
                    // Boolean support
                    if ("true".equalsIgnoreCase(firstValue) || "false".equalsIgnoreCase(firstValue)) {
                        criteriaList.add(Criteria.where(field).is(Boolean.parseBoolean(firstValue)));
                    } else {
                        criteriaList.add(Criteria.where(field).in(filter.getValue()));
                    }
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
                    Pattern pattern = Pattern.compile(firstValue, Pattern.CASE_INSENSITIVE);
                    criteriaList.add(Criteria.where(field).regex(pattern));
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
            if (!"filters".equals(rs.getField()) || rs.getValue() == null) {
                continue;
            }

            for (Object element : rs.getValue()) {
                String expr = element.toString();

                if (expr.contains("[?(")) {
                    // Array field with condition
                    String arrayField = expr.substring(0, expr.indexOf("["));
                    String conditionPart = expr.substring(expr.indexOf("[?(") + 3, expr.indexOf(")]"));

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

    private Criteria parseLogicalExpression(String expression) {
        if (expression.contains("||")) {
            String[] orParts = expression.split("\\|\\|");
            List<Criteria> orCriteriaList = Arrays.stream(orParts)
                    .map(this::parseSimpleConditions)
                    .collect(Collectors.toList());
            return new Criteria().orOperator(orCriteriaList.toArray(new Criteria[0]));
        } else if (expression.contains("&&")) {
            String[] andParts = expression.split("&&");
            List<Criteria> andCriteriaList = Arrays.stream(andParts)
                    .map(this::parseSimpleConditions)
                    .collect(Collectors.toList());
            return new Criteria().andOperator(andCriteriaList.toArray(new Criteria[0]));
        } else {
            return parseSimpleConditions(expression);
        }
    }
    private Criteria parseSimpleConditions(String condition) {
        condition = condition.trim().replaceAll("@\\.", "");

        if (condition.contains("regex==")) {
            String[] parts = condition.split("regex==");
            if (parts.length == 2) {
                String field = parts[0].trim();
                String regex = parts[1].replaceAll("'", "").trim();
                return Criteria.where(field).regex(regex);
            }
        } else if (condition.contains("!=")) {
            String[] parts = condition.split("!=");
            if (parts.length == 2) {
                String field = parts[0].trim();
                String val = parts[1].replaceAll("'", "").trim();
                return Criteria.where(field).ne(val);
            }
        } else if (condition.contains("==")) {
            String[] parts = condition.split("==");
            if (parts.length == 2) {
                String field = parts[0].trim();
                String val = parts[1].replaceAll("'", "").trim();
                return Criteria.where(field).is(val);
            }
        }

        // Fallback for unknown format
        return new Criteria();
    }

    private static Object parseValue(String value) {
        if (value.matches("^\\d+$")) {
            return Integer.parseInt(value);
        } else if (value.matches("^\\d+\\.\\d+$")) {
            return Double.parseDouble(value); // Double
        } else if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) {
            return Boolean.parseBoolean(value); // Boolean
        } else if (value.matches("^'\\d{4}-\\d{2}-\\d{2}'$")) { // Date format 'yyyy-MM-dd'
            try {
                LocalDate localDate = LocalDate.parse(value.replace("'", ""), DateTimeFormatter.ISO_LOCAL_DATE);
                return localDate.atStartOfDay().atOffset(ZoneOffset.UTC);
            } catch (DateTimeParseException e) {
                throw  e;
            }
        } else if (value.matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z$")) { // OffsetDateTime format 'yyyy-MM-ddTHH:mm:ss±HH:mm'
            try {
                return OffsetDateTime.parse(value.replace("'", ""), DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            } catch (DateTimeParseException e) {
                throw e;
            }
        } else {
            return value.replace("'", "");
        }
    }
}
