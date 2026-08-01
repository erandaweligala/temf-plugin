package com.adl.et.telco.dte.mvno.plugin.tmf.external.repositories;

import com.adl.et.telco.dte.mvno.plugin.tmf.application.config.MongoCustomConverterConfig;
import com.adl.et.telco.dte.mvno.plugin.tmf.application.exception.type.FilterException;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.BaseResourceDocument;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.Filter;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.FilterOperation;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.Page;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.Pageable;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.convert.NoOpDbRefResolver;
import org.springframework.data.mongodb.core.convert.QueryMapper;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the filter translation of {@link AbstractResourceRepository}. Queries are asserted after
 * being run through {@link QueryMapper} with the plugin's own conversions, which is what
 * {@code MongoTemplate} does before sending them to MongoDB — an {@code OffsetDateTime} is stored
 * as epoch milliseconds, so that is what a date filter has to end up as.
 */
class AbstractResourceRepositoryFilterTest {

    private static final long JAN_FIRST_2023_UTC = 1672531200000L;

    private QueryMapper queryMapper;
    private MongoMappingContext mappingContext;

    @BeforeEach
    void setUp() {
        MongoCustomConversions conversions = new MongoCustomConverterConfig().customConversions();

        mappingContext = new MongoMappingContext();
        mappingContext.setSimpleTypeHolder(conversions.getSimpleTypeHolder());
        mappingContext.afterPropertiesSet();

        MappingMongoConverter converter = new MappingMongoConverter(NoOpDbRefResolver.INSTANCE, mappingContext);
        converter.setCustomConversions(conversions);
        converter.afterPropertiesSet();

        queryMapper = new QueryMapper(converter);
    }

    @Test
    void equalityMatchesTheTypedValueSoNonStringFieldsAreFound() {
        assertThat(mappedQuery(filter("priority", FilterOperation.IS, "3")))
                .contains("\"priority\": {\"$in\": [\"3\", 3]}");

        assertThat(mappedQuery(filter("flag", FilterOperation.IS, "true")))
                .contains("\"flag\": {\"$in\": [\"true\", true]}");

        // A plain string value stays a single equality match.
        assertThat(mappedQuery(filter("state", FilterOperation.IS, "active")))
                .contains("\"state\": \"active\"");

        assertThat(mappedQuery(filter("state", FilterOperation.IS, "active", "held")))
                .contains("\"state\": {\"$in\": [\"active\", \"held\"]}");
    }

    @Test
    void comparisonOnANumberLargerThanAnIntIsAccepted() {
        assertThat(mappedQuery(filter("ts", FilterOperation.GT, "1699999999999")))
                .contains("\"ts\": {\"$gt\": 1699999999999}");
    }

    @Test
    void comparisonKeepsTheSignOfNegativeNumbers() {
        assertThat(mappedQuery(filter("amount", FilterOperation.GT, "-1.5")))
                .contains("\"amount\": {\"$gt\": -1.5}");

        assertThat(mappedQuery(filter("priority", FilterOperation.GT, "-2")))
                .contains("\"priority\": {\"$gt\": -2}");
    }

    @Test
    void datesAreComparedAsDatesWhateverSupportedFormatIsUsed() {
        assertThat(mappedQuery(filter("orderDate", FilterOperation.GT, "2023-01-01")))
                .contains("\"orderDate\": {\"$gt\": " + JAN_FIRST_2023_UTC + "}");

        assertThat(mappedQuery(filter("orderDate", FilterOperation.GT, "'2023-01-01'")))
                .contains("\"orderDate\": {\"$gt\": " + JAN_FIRST_2023_UTC + "}");

        assertThat(mappedQuery(filter("orderDate", FilterOperation.GT, "2023-01-01T00:00:00Z")))
                .contains("\"orderDate\": {\"$gt\": " + JAN_FIRST_2023_UTC + "}");

        assertThat(mappedQuery(filter("orderDate", FilterOperation.GTE, "2023-01-01T00:00:00.500Z")))
                .contains("\"orderDate\": {\"$gte\": " + (JAN_FIRST_2023_UTC + 500) + "}");

        // Offsets other than Z are honoured instead of being compared as text.
        assertThat(mappedQuery(filter("orderDate", FilterOperation.LT, "2023-01-01T00:00:00+05:30")))
                .contains("\"orderDate\": {\"$lt\": " + (JAN_FIRST_2023_UTC - 19800000L) + "}");
    }

    @Test
    void regexFilterIsCaseInsensitiveAndInvalidPatternsAreRejected() {
        assertThat(mappedQuery(filter("name", FilterOperation.REGEX, "^ab")))
                .contains("\"pattern\": \"^ab\", \"options\": \"i\"");

        assertThatThrownBy(() -> mappedQuery(filter("name", FilterOperation.REGEX, "[unclosed")))
                .isInstanceOf(FilterException.class)
                .hasMessageContaining("Invalid regular expression");
    }

    @Test
    void expressionFilterAppliesAndBeforeOr() {
        String mapped = mappedQuery(filter("filters", FilterOperation.IS, "a=='1'&&b=='2'||c=='3'"));

        assertThat(mapped).isEqualTo("{\"$and\": [{\"$or\": [{\"$and\": [{\"a\": \"1\"}, {\"b\": \"2\"}]}, "
                + "{\"c\": \"3\"}]}]}");
    }

    @Test
    void expressionFilterSupportsEqualityInequalityRegexAndElementMatch() {
        assertThat(mappedQuery(filter("filters", FilterOperation.IS, "state=='active'")))
                .contains("\"state\": \"active\"");

        assertThat(mappedQuery(filter("filters", FilterOperation.IS, "state!='active'")))
                .contains("\"state\": {\"$ne\": \"active\"}");

        assertThat(mappedQuery(filter("filters", FilterOperation.IS, "name regex=='^ab'")))
                .contains("\"pattern\": \"^ab\"");

        assertThat(mappedQuery(filter("filters", FilterOperation.IS, "items[?(@.id=='5')]")))
                .contains("\"items\": {\"$elemMatch\": {\"id\": \"5\"}}");

        assertThat(mappedQuery(filter("filters", FilterOperation.IS, "items[?(@.id=='5'||@.id=='6')]")))
                .contains("\"$elemMatch\": {\"$or\": [{\"id\": \"5\"}, {\"id\": \"6\"}]}");
    }

    @Test
    void unparseableExpressionIsRejectedInsteadOfMatchingEveryDocument() {
        assertThatThrownBy(() -> mappedQuery(filter("filters", FilterOperation.IS, "garbage")))
                .isInstanceOf(FilterException.class)
                .hasMessageContaining("Unsupported filter expression");

        assertThatThrownBy(() -> mappedQuery(filter("filters", FilterOperation.IS, "items[0]")))
                .isInstanceOf(FilterException.class);

        assertThatThrownBy(() -> mappedQuery(filter("filters", FilterOperation.IS, "items[?(@.id=='5'")))
                .isInstanceOf(FilterException.class)
                .hasMessageContaining("Unterminated");
    }

    @Test
    void filterWithoutValuesOrOperationIsIgnored() {
        assertThat(mappedQuery(filter("state", FilterOperation.IS))).isEqualTo("{}");
        assertThat(mappedQuery(new Filter("state", new Object[]{"active"}, null))).isEqualTo("{}");
    }

    @Test
    void pagingStagesAreOnlyAddedWhenTheyAreMeaningful() {
        assertThat(pipelineFor(new Repo(pagingTemplate()), new Pageable(0, 10)))
                .doesNotContain("$skip")
                .contains("\"$limit\" : 10");

        // Offset without a limit still pages, and never sends a $limit of zero.
        assertThat(pipelineFor(new Repo(pagingTemplate()), new Pageable(20, 0)))
                .contains("\"$skip\" : 20")
                .doesNotContain("$limit");
    }

    @Test
    void customAggregationOperationsAreAppliedToResultsAndCount() {
        MongoTemplate template = pagingTemplate();

        Page<Doc> page = new TenantRepo(template).query(
                List.of(filter("state", FilterOperation.IS, "active")), null, new Pageable(0, 5));

        ArgumentCaptor<Aggregation> results = ArgumentCaptor.forClass(Aggregation.class);
        verify(template).aggregate(results.capture(), eq(Doc.class), eq(Doc.class));
        assertThat(results.getValue().toString()).contains("\"tenant\" : \"t1\"");

        ArgumentCaptor<Aggregation> count = ArgumentCaptor.forClass(Aggregation.class);
        verify(template).aggregate(count.capture(), eq(Doc.class), eq(Document.class));
        assertThat(count.getValue().toString())
                .contains("\"tenant\" : \"t1\"")
                .contains("$count");

        assertThat(page.getTotal()).isEqualTo(7);
    }

    private static Filter filter(String field, FilterOperation operation, Object... values) {
        return new Filter(field, values, operation);
    }

    /**
     * Run a query and return the resulting MongoDB query as it would be sent to the server.
     */
    private String mappedQuery(Filter... filters) {
        MongoTemplate template = mock(MongoTemplate.class);
        when(template.find(any(Query.class), eq(Doc.class))).thenReturn(Collections.emptyList());

        new Repo(template).query(List.of(filters), null, new Pageable());

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(template).find(captor.capture(), eq(Doc.class));

        return queryMapper.getMappedObject(captor.getValue().getQueryObject(),
                mappingContext.getPersistentEntity(Doc.class)).toJson();
    }

    @SuppressWarnings("unchecked")
    private MongoTemplate pagingTemplate() {
        MongoTemplate template = mock(MongoTemplate.class);

        AggregationResults<Doc> results = mock(AggregationResults.class);
        when(results.getMappedResults()).thenReturn(Collections.emptyList());
        when(template.aggregate(any(Aggregation.class), eq(Doc.class), eq(Doc.class))).thenReturn(results);

        AggregationResults<Document> counts = mock(AggregationResults.class);
        when(counts.getUniqueMappedResult()).thenReturn(new Document("total", 7));
        when(template.aggregate(any(Aggregation.class), eq(Doc.class), eq(Document.class))).thenReturn(counts);

        when(template.count(any(Query.class), eq(Doc.class))).thenReturn(3L);

        return template;
    }

    private String pipelineFor(Repo repository, Pageable pageable) {

        repository.query(List.of(filter("state", FilterOperation.IS, "active")), null, pageable);

        ArgumentCaptor<Aggregation> captor = ArgumentCaptor.forClass(Aggregation.class);
        verify(repository.template).aggregate(captor.capture(), eq(Doc.class), eq(Doc.class));

        return captor.getValue().toString();
    }

    private static class Doc extends BaseResourceDocument {

        private String state;
        private boolean flag;
        private int priority;
        private long ts;
        private double amount;
        private String name;
        private OffsetDateTime orderDate;
    }

    private static class Repo extends AbstractResourceRepository<Doc> {

        private final MongoTemplate template;

        Repo(MongoTemplate template) {
            super(template, Doc.class, Runnable::run);
            this.template = template;
        }
    }

    private static class TenantRepo extends AbstractResourceRepository<Doc> {

        TenantRepo(MongoTemplate template) {
            super(template, Doc.class, Runnable::run);
        }

        @Override
        protected void addOperations(List<AggregationOperation> operations) {
            operations.add(Aggregation.match(Criteria.where("tenant").is("t1")));
        }
    }
}
