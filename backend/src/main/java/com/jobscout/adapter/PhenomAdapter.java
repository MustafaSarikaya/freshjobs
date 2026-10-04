package com.jobscout.adapter;

import com.jobscout.domain.ListingStatus;
import com.jobscout.domain.Platform;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class PhenomAdapter implements Adapter {

    private static final int PAGE_SIZE = 20;

    private final RestClient http;

    PhenomAdapter(@Qualifier("adapterRestClient") RestClient http) {
        this.http = http;
    }

    @Override
    public Platform platform() { return Platform.PHENOM; }

    @Override
    @SuppressWarnings("unchecked")
    public List<FetchedListing> fetchListings(String boardId) {
        String[] parts = parseBoardId(boardId);
        String basePath = parts[0];
        String refNum = parts[1];
        String domain = basePath.split("/", 2)[0];

        List<FetchedListing> out = new ArrayList<>();
        int offset = 0;
        while (true) {
            Map<String, Object> request = new LinkedHashMap<>();
            request.put("lang", "en_global");
            request.put("deviceType", "desktop");
            request.put("country", "global");
            request.put("pageName", "search-results");
            request.put("ddoKey", "refineSearch");
            request.put("refNum", refNum);
            request.put("size", PAGE_SIZE);
            request.put("from", offset);
            request.put("jobs", true);
            request.put("counts", true);
            request.put("keywords", "");

            Map<String, Object> resp = http.post()
                .uri("https://" + domain + "/widgets")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(Map.class);

            Map<String, Object> payload = (Map<String, Object>)
                (resp == null ? Map.of() : resp.getOrDefault("refineSearch", Map.of()));
            Map<String, Object> data = (Map<String, Object>)
                payload.getOrDefault("data", Map.of());
            List<Map<String, Object>> jobs = (List<Map<String, Object>>)
                data.getOrDefault("jobs", List.of());
            if (jobs.isEmpty()) break;

            for (Map<String, Object> job : jobs) {
                List<String> locations;
                Object multi = job.get("multi_location_array");
                if (multi instanceof List<?> ml && !ml.isEmpty()) {
                    locations = ml.stream()
                        .map(e -> e instanceof Map<?, ?> m ? m.get("location") : e)
                        .filter(v -> v instanceof String s && !s.isBlank())
                        .map(Object::toString)
                        .toList();
                } else if (job.get("multi_location") instanceof List<?> oldStyle
                    && !oldStyle.isEmpty()) {
                    locations = oldStyle.stream().map(Object::toString).toList();
                } else if (job.get("location") instanceof String s && !s.isBlank()) {
                    locations = List.of(s);
                } else {
                    locations = List.of();
                }

                out.add(new FetchedListing(
                    String.valueOf(job.get("jobId")),
                    (String) job.get("title"),
                    "https://" + basePath + "/job/" + job.get("jobId"),
                    locations,
                    ListingStatus.ACTIVE,
                    parseInstant((String) job.get("postedDate")),
                    parseInstant((String) job.get("dateCreated"))
                ));
            }

            offset += PAGE_SIZE;
            Object total = payload.get("totalHits");
            if (total instanceof Number n && offset >= n.longValue()) break;
        }
        return out;
    }

    static String[] parseBoardId(String boardId) {
        if (boardId == null) throw new IllegalArgumentException("Invalid Phenom boardId");
        String[] parts = boardId.split(":", 2);
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new IllegalArgumentException(
                "Invalid Phenom boardId " + boardId + ": expected 'base_path:refNum'");
        }
        return parts;
    }

    private static Instant parseInstant(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Instant.parse(s); }
        catch (Exception e) { return null; }
    }
}
