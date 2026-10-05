package com.jobscout.api;

import com.jobscout.api.generated.api.RunsApi;
import com.jobscout.api.generated.model.Run;
import com.jobscout.api.generated.model.RunPage;
import com.jobscout.api.generated.model.TriggerRunRequest;
import com.jobscout.ingestion.AsyncIngestionRunner;
import com.jobscout.mapper.RunMapper;
import com.jobscout.repository.RunRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1")
class RunsController implements RunsApi {

    private final RunRepository runs;
    private final AsyncIngestionRunner runner;
    private final String ingestApiKey;

    RunsController(RunRepository runs,
                   AsyncIngestionRunner runner,
                   @Value("${jobscout.ingest-api-key:}") String ingestApiKey) {
        this.runs = runs;
        this.runner = runner;
        this.ingestApiKey = ingestApiKey;
    }

    @Override
    public ResponseEntity<RunPage> listRuns(Integer page, Integer pageSize) {
        var p = runs.findAll(PageRequest.of(
            page == null ? 0 : page,
            pageSize == null ? 50 : pageSize,
            Sort.by(Sort.Direction.DESC, "createdAt")));
        return ResponseEntity.ok(RunMapper.toPage(p));
    }

    @Override
    public ResponseEntity<Run> getRun(Long id) {
        return runs.findById(id)
            .map(r -> ResponseEntity.ok(RunMapper.toDto(r)))
            .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @Override
    public ResponseEntity<Run> triggerRun(TriggerRunRequest body) {
        authorizeTriggerRun();
        com.jobscout.domain.Run created = new com.jobscout.domain.Run();
        created = runs.save(created);
        Long id = created.getId();
        runner.run(id);
        return ResponseEntity.accepted()
            .header("Location", "/api/v1/runs/" + id)
            .body(RunMapper.toDto(created));
    }

    private void authorizeTriggerRun() {
        if (isAdmin(SecurityContextHolder.getContext().getAuthentication())) {
            return;
        }

        if (!ingestApiKey.isBlank()) {
            String header = currentRequest().getHeader("Authorization");
            String expected = "Bearer " + ingestApiKey;
            if (constantTimeEquals(header, expected)) {
                return;
            }
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
    }

    private static boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch(a -> a.equals("ROLE_STAFF") || a.equals("ROLE_SUPERUSER"));
    }

    private static HttpServletRequest currentRequest() {
        ServletRequestAttributes attributes =
            (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            throw new IllegalStateException("No current servlet request");
        }
        return attributes.getRequest();
    }

    private static boolean constantTimeEquals(String candidate, String expected) {
        if (candidate == null) {
            return false;
        }
        return MessageDigest.isEqual(
            candidate.getBytes(StandardCharsets.UTF_8),
            expected.getBytes(StandardCharsets.UTF_8));
    }
}
