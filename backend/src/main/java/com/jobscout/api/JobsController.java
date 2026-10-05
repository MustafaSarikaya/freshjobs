package com.jobscout.api;

import com.jobscout.api.generated.api.JobsApi;
import com.jobscout.api.generated.model.FacetBucket;
import com.jobscout.api.generated.model.JobListingPage;
import com.jobscout.api.generated.model.MarkSeenResponse;
import com.jobscout.domain.JobListing;
import com.jobscout.filter.FilterExpressionEvaluator;
import com.jobscout.mapper.JobListingMapper;
import com.jobscout.repository.JobListingRepository;
import com.jobscout.service.JobFacetsService;
import com.jobscout.service.SortParser;
import com.jobscout.session.SessionPreferences;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
class JobsController implements JobsApi {

    private final JobListingRepository listings;
    private final FilterExpressionEvaluator filterEval;
    private final JobFacetsService facetsService;
    private final SessionPreferences prefs;

    JobsController(JobListingRepository listings,
                   FilterExpressionEvaluator filterEval,
                   JobFacetsService facetsService,
                   SessionPreferences prefs) {
        this.listings = listings;
        this.filterEval = filterEval;
        this.facetsService = facetsService;
        this.prefs = prefs;
    }

    // Spring Boot runs with open-in-view=false, so the JPA session closes
    // when each repository call returns. JobListingMapper walks the
    // (lazy) source and locations associations on each entity, which
    // throws LazyInitializationException once the session is gone. Wrap
    // the read + map in a single read-only transaction so the proxies
    // stay loadable through serialization.
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<JobListingPage> listJobs(
        Integer page, Integer pageSize, List<String> sort, String filter
    ) {
        Sort sortObj = SortParser.parse(sort);
        Specification<JobListing> spec = filterEval.toSpecification(filter);

        Page<JobListing> result = listings.findAll(
            spec,
            PageRequest.of(page == null ? 0 : page,
                           pageSize == null ? 50 : pageSize,
                           sortObj));

        var seenIds = prefs.seenAmong(
            result.getContent().stream().map(JobListing::getId).toList());

        return ResponseEntity.ok(JobListingMapper.toPage(result, seenIds));
    }

    @Override
    public ResponseEntity<Map<String, List<FacetBucket>>> getJobFacets(
        List<String> fields, String filter
    ) {
        Specification<JobListing> spec = filterEval.toSpecification(filter);
        return ResponseEntity.ok(facetsService.compute(fields, spec));
    }

    @Override
    public ResponseEntity<MarkSeenResponse> markJobSeen(Long id) {
        JobListing listing = listings.findById(id)
            .orElseThrow(() -> new ResponseStatusException(
                org.springframework.http.HttpStatus.NOT_FOUND, "Job not found"));
        prefs.markSeen(listing.getId());
        return ResponseEntity.ok(new MarkSeenResponse(listing.getId(), true));
    }
}
