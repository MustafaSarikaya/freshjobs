package com.jobscout.ingestion;

import com.jobscout.adapter.Adapter;
import com.jobscout.adapter.AdapterRegistry;
import com.jobscout.adapter.FetchedListing;
import com.jobscout.domain.JobListing;
import com.jobscout.domain.ListingStatus;
import com.jobscout.domain.LocationTag;
import com.jobscout.domain.Run;
import com.jobscout.domain.RunStatus;
import com.jobscout.domain.Source;
import com.jobscout.geo.LocationNormalizationService;
import com.jobscout.repository.JobListingRepository;
import com.jobscout.repository.LocationTagRepository;
import com.jobscout.repository.RunRepository;
import com.jobscout.repository.SourceRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);

    private final SourceRepository sources;
    private final JobListingRepository listings;
    private final LocationTagRepository locations;
    private final RunRepository runs;
    private final AdapterRegistry adapters;
    private final LocationNormalizationService normalizer;
    private final TransactionTemplate perSourceTx;

    public IngestionService(SourceRepository sources,
                            JobListingRepository listings,
                            LocationTagRepository locations,
                            RunRepository runs,
                            AdapterRegistry adapters,
                            LocationNormalizationService normalizer,
                            PlatformTransactionManager txManager) {
        this.sources = sources;
        this.listings = listings;
        this.locations = locations;
        this.runs = runs;
        this.adapters = adapters;
        this.normalizer = normalizer;
        // Per-source transaction so one bad source doesn't roll back the others.
        // Explicit template rather than @Transactional: ingestAll calls into
        // this same bean, which would bypass Spring's proxy.
        this.perSourceTx = new TransactionTemplate(txManager);
        this.perSourceTx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /**
     * Run ingestion across every active source. Creates a {@link Run} row,
     * updates it as each source completes, and returns the aggregated result.
     */
    public IngestionResult ingestAll(Long runId) {
        Run run = runId == null
            ? runs.save(newRun(RunStatus.RUNNING))
            : runs.findById(runId).orElseGet(() -> runs.save(newRun(RunStatus.RUNNING)));
        run.setStartedAt(Instant.now());
        run.setStatus(RunStatus.RUNNING);
        runs.save(run);

        IngestionResult result = new IngestionResult();
        for (Source source : sources.findByActiveTrue()) {
            try {
                IngestionResult sourceResult = processSource(source);
                result.listingsCreated += sourceResult.listingsCreated;
                result.listingsUpdated += sourceResult.listingsUpdated;
                result.listingsExpired += sourceResult.listingsExpired;
                result.sourcesProcessed++;
            } catch (Exception e) {
                String msg = source.getName() + ": " + e.getMessage();
                log.error("Failed to ingest {}: {}", source.getName(), e.getMessage(), e);
                result.errors.add(msg);
            }
        }

        run.setFinishedAt(Instant.now());
        run.setStatus(result.hasErrors() ? RunStatus.FAILED : RunStatus.COMPLETED);
        run.setSourcesProcessed(result.sourcesProcessed);
        run.setListingsCreated(result.listingsCreated);
        run.setListingsUpdated(result.listingsUpdated);
        run.setListingsExpired(result.listingsExpired);
        if (result.hasErrors()) run.setErrorMessage(String.join("\n", result.errors));
        runs.save(run);
        return result;
    }

    /**
     * Fetch outside any transaction (some boards take a minute), then persist
     * in a fresh transaction. Counts are only reported if the commit succeeds.
     */
    IngestionResult processSource(Source source) {
        Adapter adapter = adapters.get(source.getPlatform());
        List<FetchedListing> fetched = adapter.fetchListings(source.getBoardId());
        log.info("Source {} returned {} listings", source.getName(), fetched.size());

        IngestionResult result = new IngestionResult();
        perSourceTx.executeWithoutResult(status -> persist(source, fetched, result));
        return result;
    }

    private void persist(Source source, List<FetchedListing> fetched, IngestionResult result) {
        Instant now = Instant.now();
        String profile = normalizer.parsingProfile(source.getPlatform(), source.getBoardId());

        Set<String> fetchedIds = new HashSet<>(fetched.size());
        for (FetchedListing item : fetched) {
            fetchedIds.add(item.externalId());
            JobListing listing = listings.findBySourceAndExternalId(source, item.externalId())
                .orElse(null);
            boolean created = (listing == null);
            if (created) {
                listing = new JobListing();
                listing.setSource(source);
                listing.setExternalId(item.externalId());
            }
            listing.setTitle(item.title());
            listing.setUrl(item.url());
            listing.setPublishedAt(item.publishedAt());
            listing.setUpdatedAtSource(item.updatedAtSource());

            // The adapter may mark expired (Ashby's is_listed=false case);
            // otherwise the listing is active.
            if (item.status() == ListingStatus.EXPIRED && listing.getStatus() == ListingStatus.ACTIVE) {
                listing.setStatus(ListingStatus.EXPIRED);
                listing.setExpiredAt(now);
                if (!created) result.listingsExpired++;
            } else {
                listing.setStatus(ListingStatus.ACTIVE);
                listing.setExpiredAt(null);
            }

            listing = listings.save(listing);
            syncLocations(listing, item.locations(), profile);

            if (created) result.listingsCreated++;
            else result.listingsUpdated++;
        }

        // Mark anything still active for this source but missing from the fetch
        // as expired.
        List<JobListing> staleActives = listings.findAll((root, q, cb) -> cb.and(
            cb.equal(root.get("source"), source),
            cb.equal(root.get("status"), ListingStatus.ACTIVE),
            cb.not(root.get("externalId").in(fetchedIds.isEmpty() ? List.of("") : fetchedIds))
        ));
        for (JobListing stale : staleActives) {
            stale.setStatus(ListingStatus.EXPIRED);
            stale.setExpiredAt(now);
            listings.save(stale);
            result.listingsExpired++;
        }
    }

    private void syncLocations(JobListing listing, List<String> rawNames, String profile) {
        List<String> normalized = normalizer.normalizeValues(rawNames, profile);
        if (normalized.isEmpty()) {
            if (!listing.getLocations().isEmpty()) listing.getLocations().clear();
            return;
        }
        // Look up existing tags in one query, create the rest.
        List<LocationTag> existing = locations.findByNameIn(normalized);
        Map<String, LocationTag> byName = new HashMap<>(existing.size());
        for (LocationTag t : existing) byName.put(t.getName(), t);

        List<LocationTag> tags = new ArrayList<>(normalized.size());
        for (String name : normalized) {
            LocationTag tag = byName.get(name);
            if (tag == null) {
                tag = new LocationTag();
                tag.setName(name);
                tag = locations.save(tag);
            }
            tags.add(tag);
        }
        listing.getLocations().clear();
        listing.getLocations().addAll(tags);
    }

    private static Run newRun(RunStatus status) {
        Run r = new Run();
        r.setStatus(status);
        return r;
    }
}
