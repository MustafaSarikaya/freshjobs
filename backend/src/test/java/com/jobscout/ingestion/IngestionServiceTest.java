package com.jobscout.ingestion;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jobscout.adapter.Adapter;
import com.jobscout.adapter.AdapterRegistry;
import com.jobscout.adapter.FetchedListing;
import com.jobscout.domain.JobListing;
import com.jobscout.domain.ListingStatus;
import com.jobscout.domain.Platform;
import com.jobscout.domain.Run;
import com.jobscout.domain.Source;
import com.jobscout.geo.LocationNormalizationService;
import com.jobscout.repository.JobListingRepository;
import com.jobscout.repository.LocationTagRepository;
import com.jobscout.repository.RunRepository;
import com.jobscout.repository.SourceRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

class IngestionServiceTest {

    private final SourceRepository sources = mock(SourceRepository.class);
    private final JobListingRepository listings = mock(JobListingRepository.class);
    private final LocationTagRepository locations = mock(LocationTagRepository.class);
    private final RunRepository runs = mock(RunRepository.class);
    private final Adapter adapter = mock(Adapter.class);
    private final LocationNormalizationService normalizer = mock(LocationNormalizationService.class);
    private final PlatformTransactionManager txManager = mock(PlatformTransactionManager.class);

    private IngestionService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        when(adapter.platform()).thenReturn(Platform.GREENHOUSE);
        when(runs.save(any(Run.class))).thenAnswer(inv -> inv.getArgument(0));
        when(listings.save(any(JobListing.class))).thenAnswer(inv -> inv.getArgument(0));
        when(listings.findBySourceAndExternalId(any(), anyString())).thenReturn(Optional.empty());
        when(listings.findAll(any(Specification.class))).thenReturn(List.of());
        when(normalizer.normalizeValues(anyList(), any())).thenReturn(List.of());
        when(txManager.getTransaction(any())).thenAnswer(inv -> new SimpleTransactionStatus());

        service = new IngestionService(sources, listings, locations, runs,
            new AdapterRegistry(List.of(adapter)), normalizer, txManager);
    }

    @Test
    void persistsEachSourceInsideItsOwnNewTransaction() {
        Source acme = source("acme");
        when(sources.findByActiveTrue()).thenReturn(List.of(acme));
        when(adapter.fetchListings("acme")).thenReturn(List.of(listing("1")));

        IngestionResult result = service.ingestAll(null);

        assertThat(result.listingsCreated).isEqualTo(1);
        InOrder order = inOrder(adapter, txManager, listings);
        order.verify(adapter).fetchListings("acme");
        order.verify(txManager).getTransaction(argThat(def ->
            def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        order.verify(listings).save(any(JobListing.class));
        order.verify(txManager).commit(any(TransactionStatus.class));
    }

    @Test
    void rollsBackFailedSourceWithoutCountingItsListings() {
        Source bad = source("bad");
        when(sources.findByActiveTrue()).thenReturn(List.of(bad));
        when(adapter.fetchListings("bad")).thenReturn(List.of(listing("1")));
        when(listings.save(any(JobListing.class))).thenThrow(new IllegalStateException("boom"));

        IngestionResult result = service.ingestAll(null);

        verify(txManager).rollback(any(TransactionStatus.class));
        verify(txManager, never()).commit(any(TransactionStatus.class));
        assertThat(result.listingsCreated).isZero();
        assertThat(result.sourcesProcessed).isZero();
        assertThat(result.errors).singleElement().asString().contains("bad", "boom");
    }

    private static Source source(String boardId) {
        Source s = new Source();
        s.setName(boardId);
        s.setPlatform(Platform.GREENHOUSE);
        s.setBoardId(boardId);
        return s;
    }

    private static FetchedListing listing(String id) {
        return new FetchedListing(id, "Job " + id, "https://example.com/" + id,
            List.of("Toronto"), ListingStatus.ACTIVE, null, null);
    }
}
