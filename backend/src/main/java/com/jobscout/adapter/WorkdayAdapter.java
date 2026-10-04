package com.jobscout.adapter;

import com.jobscout.domain.ListingStatus;
import com.jobscout.domain.Platform;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class WorkdayAdapter implements Adapter {

    private static final int PAGE_SIZE = 20;
    private static final int DETAIL_MAX_ATTEMPTS = 3;
    private static final Pattern SUMMARY_LABEL =
        Pattern.compile("^\\s*\\d+\\s+Locations?\\s*$");

    private final RestClient listClient;
    private final RestClient detailClient;

    WorkdayAdapter(@Qualifier("adapterRestClient") RestClient listClient,
                   @Qualifier("detailRestClient") RestClient detailClient) {
        this.listClient = listClient;
        this.detailClient = detailClient;
    }

    @Override
    public Platform platform() { return Platform.WORKDAY; }

    @Override
    @SuppressWarnings("unchecked")
    public List<FetchedListing> fetchListings(String boardId) {
        String[] parts = parseBoardId(boardId);
        String tenant = parts[0], cluster = parts[1], site = parts[2];
        String host = "https://" + tenant + "." + cluster + ".myworkdayjobs.com";
        String listUrl = host + "/wday/cxs/" + tenant + "/" + site + "/jobs";
        String detailBase = host + "/wday/cxs/" + tenant + "/" + site;

        List<FetchedListing> listings = new ArrayList<>();
        // Postings whose location text matches the summary-label pattern;
        // we'll resolve real locations via the detail endpoint concurrently.
        List<int[]> pendingDetail = new ArrayList<>();
        List<String> pendingPath = new ArrayList<>();

        String jobUrlBase = host + "/" + site;
        long knownTotal = -1;
        int offset = 0;
        while (true) {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("appliedFacets", Map.of());
            body.put("limit", PAGE_SIZE);
            body.put("offset", offset);
            body.put("searchText", "");

            Map<String, Object> data = listClient.post()
                .uri(listUrl)
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(Map.class);

            List<Map<String, Object>> postings = data == null
                ? List.of()
                : (List<Map<String, Object>>) data.getOrDefault("jobPostings", List.of());
            if (postings.isEmpty()) break;

            for (Map<String, Object> posting : postings) {
                String externalPath = (String) posting.getOrDefault("externalPath", "");
                String externalId = externalIdFor(posting, externalPath);
                String locationsText = (String) posting.get("locationsText");

                List<String> locations;
                if (locationsText != null && SUMMARY_LABEL.matcher(locationsText).matches()) {
                    pendingDetail.add(new int[]{listings.size()});
                    pendingPath.add(externalPath);
                    locations = List.of();
                } else if (locationsText != null && !locationsText.isBlank()) {
                    locations = List.of(locationsText);
                } else {
                    locations = List.of();
                }

                listings.add(new FetchedListing(
                    externalId,
                    (String) posting.get("title"),
                    jobUrlBase + externalPath,
                    locations,
                    ListingStatus.ACTIVE,
                    null,
                    null
                ));
            }

            offset += PAGE_SIZE;
            if (knownTotal < 0 && data.get("total") instanceof Number n && n.longValue() > 0) {
                knownTotal = n.longValue();
            }
            if (postings.size() < PAGE_SIZE) break;
            if (knownTotal >= 0 && offset >= knownTotal) break;
        }

        if (!pendingDetail.isEmpty()) {
            resolveDetails(detailBase, pendingDetail, pendingPath, listings);
        }
        return listings;
    }

    private void resolveDetails(
        String detailBase,
        List<int[]> pendingDetail,
        List<String> pendingPath,
        List<FetchedListing> listings
    ) {
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<List<String>>> futures = new ArrayList<>(pendingDetail.size());
            for (String path : pendingPath) {
                futures.add(pool.submit(() -> fetchDetailLocations(detailBase, path)));
            }
            for (int i = 0; i < pendingDetail.size(); i++) {
                List<String> resolved = List.of();
                try {
                    resolved = futures.get(i).get();
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                } catch (ExecutionException ee) {
                    // Already swallowed inside fetchDetailLocations; keep empty.
                }
                int idx = pendingDetail.get(i)[0];
                FetchedListing original = listings.get(idx);
                listings.set(idx, new FetchedListing(
                    original.externalId(),
                    original.title(),
                    original.url(),
                    resolved,
                    original.status(),
                    original.publishedAt(),
                    original.updatedAtSource()
                ));
            }
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> fetchDetailLocations(String detailBase, String externalPath) {
        if (externalPath == null || externalPath.isBlank()) return List.of();
        String url = detailBase + externalPath;
        for (int attempt = 0; attempt < DETAIL_MAX_ATTEMPTS; attempt++) {
            try {
                Map<String, Object> resp = detailClient.get()
                    .uri(url)
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(Map.class);
                Map<String, Object> info = (Map<String, Object>)
                    (resp == null ? Map.of() : resp.getOrDefault("jobPostingInfo", Map.of()));
                List<Object> additional = (List<Object>)
                    info.getOrDefault("additionalLocations", List.of());
                return dedupe(info.get("location"), additional);
            } catch (RestClientException e) {
                // retryable network/HTTP error; loop
            }
        }
        return List.of();
    }

    private static List<String> dedupe(Object primary, List<Object> additional) {
        Set<String> seen = new LinkedHashSet<>();
        addIfString(seen, primary);
        if (additional != null) for (Object item : additional) addIfString(seen, item);
        return List.copyOf(seen);
    }

    private static void addIfString(Set<String> sink, Object item) {
        if (item instanceof String s) {
            String trimmed = s.strip();
            if (!trimmed.isEmpty()) sink.add(trimmed);
        }
    }

    @SuppressWarnings("unchecked")
    private static String externalIdFor(Map<String, Object> posting, String externalPath) {
        List<Object> bullets = (List<Object>) posting.getOrDefault("bulletFields", List.of());
        if (!bullets.isEmpty()) return String.valueOf(bullets.get(0));
        int slash = externalPath.lastIndexOf('/');
        return slash >= 0 ? externalPath.substring(slash + 1) : externalPath;
    }

    static String[] parseBoardId(String boardId) {
        if (boardId == null) throw new IllegalArgumentException("Invalid Workday boardId");
        String[] parts = boardId.split(":");
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            throw new IllegalArgumentException(
                "Invalid Workday boardId " + boardId + ": expected 'tenant:cluster:site'");
        }
        return parts;
    }
}
