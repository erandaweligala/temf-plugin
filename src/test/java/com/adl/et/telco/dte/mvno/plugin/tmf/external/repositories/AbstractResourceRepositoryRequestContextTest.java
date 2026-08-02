package com.adl.et.telco.dte.mvno.plugin.tmf.external.repositories;

import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.BaseResourceDocument;
import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.Pageable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * A paged query runs the results and the count on the shared executor. The service layer resolves
 * the collection name from the tenant and catalog headers through the request scoped
 * {@code HttpServletRequest}, which is bound to the request thread only, so the request context
 * has to be carried over to the executor threads.
 */
class AbstractResourceRepositoryRequestContextTest {

    private static final String TENANT_HEADER = "tenantId";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @BeforeEach
    void bindRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(TENANT_HEADER, "tenant-a");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @AfterEach
    void unbindRequest() {
        RequestContextHolder.resetRequestAttributes();
        executor.shutdownNow();
    }

    @Test
    void requestHeadersAreReadableWhileTheQueryRunsOnTheExecutor() {
        AtomicReference<String> tenantSeenByQuery = new AtomicReference<>();

        new Repo(pagingTemplate(tenantSeenByQuery), executor).query(null, null, new Pageable(0, 10));

        assertThat(tenantSeenByQuery.get()).isEqualTo("tenant-a");
    }

    @Test
    void theRequestIsNotLeftBehindOnTheExecutorThread() {
        new Repo(pagingTemplate(new AtomicReference<>()), executor).query(null, null, new Pageable(0, 10));

        Boolean stillBound = CompletableFuture
                .supplyAsync(() -> RequestContextHolder.getRequestAttributes() != null, executor)
                .join();

        assertThat(stillBound).isFalse();
    }

    @Test
    void aQueryWithoutABoundRequestStillRuns() {
        RequestContextHolder.resetRequestAttributes();

        assertThat(new Repo(pagingTemplate(new AtomicReference<>()), executor)
                .query(null, null, new Pageable(0, 10)).getTotal()).isEqualTo(3);
    }

    /**
     * Answers the paged query and records the tenant header as seen from the executor thread.
     */
    @SuppressWarnings("unchecked")
    private MongoTemplate pagingTemplate(AtomicReference<String> tenantSeenByQuery) {
        MongoTemplate template = mock(MongoTemplate.class);

        AggregationResults<Doc> results = mock(AggregationResults.class);
        when(results.getMappedResults()).thenReturn(Collections.emptyList());
        when(template.aggregate(any(Aggregation.class), eq(Doc.class), eq(Doc.class)))
                .thenAnswer(invocation -> {
                    tenantSeenByQuery.set(currentTenantHeader());
                    return results;
                });

        when(template.count(any(Query.class), eq(Doc.class))).thenReturn(3L);

        return template;
    }

    private static String currentTenantHeader() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        return attributes == null ? null
                : ((ServletRequestAttributes) attributes).getRequest().getHeader(TENANT_HEADER);
    }

    private static class Doc extends BaseResourceDocument {
    }

    private static class Repo extends AbstractResourceRepository<Doc> {

        Repo(MongoTemplate template, ExecutorService executor) {
            super(template, Doc.class, executor);
        }
    }
}
