package com.adl.et.telco.dte.mvno.plugin.tmf.external.repositories;

import com.adl.et.telco.dte.mvno.plugin.tmf.domain.entities.BaseResourceDocument;
import com.adl.et.telco.dte.mvno.plugin.tmf.external.repositories.utils.MongoSequenceEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Covers the pre-allocated ID block behaviour of {@link AbstractResourceRepository#retrieveNextId()}:
 * it should serve IDs from an in-memory block and only reach MongoDB once per block, while still
 * returning unique, gap-free IDs under concurrent access.
 */
class AbstractResourceRepositoryIdBlockTest {

    private static final long ID_BLOCK_SIZE = 100;
    private static final String PREFIX = "RES-";

    private MongoTemplate mongoTemplate;
    private AtomicLong sharedCounter;
    private TestRepository repository;

    @BeforeEach
    void setUp() {
        mongoTemplate = mock(MongoTemplate.class);
        sharedCounter = new AtomicLong(0);
        repository = new TestRepository(mongoTemplate);

        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                eq(MongoSequenceEntity.class), anyString()))
                .thenAnswer(invocation -> {
                    long previous = sharedCounter.getAndAdd(ID_BLOCK_SIZE);
                    MongoSequenceEntity sequenceEntity = new MongoSequenceEntity();
                    sequenceEntity.setName(TestDocument.class.getSimpleName());
                    sequenceEntity.setPrefix(PREFIX);
                    sequenceEntity.setCurrent(previous);
                    return sequenceEntity;
                });
    }

    @Test
    void sequentialCallsWithinOneBlockUseASingleMongoRoundTrip() {
        List<String> ids = IntStream.range(0, 50)
                .mapToObj(i -> repository.retrieveNextId())
                .collect(Collectors.toList());

        assertThat(ids).hasSize(50);
        assertThat(new HashSet<>(ids)).hasSize(50);
        verify(mongoTemplate, times(1))
                .findAndModify(any(Query.class), any(Update.class), eq(MongoSequenceEntity.class), anyString());
    }

    @Test
    void crossingABlockBoundaryTriggersExactlyOneMoreMongoCall() {
        for (int i = 0; i < ID_BLOCK_SIZE; i++) {
            repository.retrieveNextId();
        }
        verify(mongoTemplate, times(1))
                .findAndModify(any(Query.class), any(Update.class), eq(MongoSequenceEntity.class), anyString());

        String nextId = repository.retrieveNextId();

        assertThat(nextId).isEqualTo(PREFIX + ID_BLOCK_SIZE);
        verify(mongoTemplate, times(2))
                .findAndModify(any(Query.class), any(Update.class), eq(MongoSequenceEntity.class), anyString());
    }

    @Test
    void missingSequenceDocumentStillReturnsNull() {
        reset(mongoTemplate);
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class),
                eq(MongoSequenceEntity.class), anyString()))
                .thenReturn(null);

        assertThat(repository.retrieveNextId()).isNull();
    }

    @Test
    void concurrentCallersNeverReceiveDuplicateIds() throws InterruptedException {
        int threadCount = 20;
        int idsPerThread = 50;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        ConcurrentLinkedQueue<String> ids = new ConcurrentLinkedQueue<>();
        CountDownLatch latch = new CountDownLatch(threadCount);

        for (int t = 0; t < threadCount; t++) {
            pool.submit(() -> {
                try {
                    for (int i = 0; i < idsPerThread; i++) {
                        ids.add(repository.retrieveNextId());
                    }
                } finally {
                    latch.countDown();
                }
            });
        }

        assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
        pool.shutdown();

        Set<String> uniqueIds = new HashSet<>(ids);
        assertThat(ids).hasSize(threadCount * idsPerThread);
        assertThat(uniqueIds).hasSize(threadCount * idsPerThread);
    }

    private static class TestDocument extends BaseResourceDocument {
    }

    private static class TestRepository extends AbstractResourceRepository<TestDocument> {
        TestRepository(MongoTemplate mongoTemplate) {
            super(mongoTemplate, TestDocument.class, Runnable::run);
        }
    }
}
