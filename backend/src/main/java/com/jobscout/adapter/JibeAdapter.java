package com.jobscout.adapter;

import com.jobscout.domain.ListingStatus;
import com.jobscout.domain.Platform;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
class JibeAdapter implements Adapter {

    private static final int PAGE_SIZE = 100;

    private final RestClient http;

    JibeAdapter(@Qualifier("adapterRestClient") RestClient http) {
        this.http = http;
    }

    @Override
    public Platform platform() { return Platform.JIBE; }

    @Override
    @SuppressWarnings("unchecked")
    public List<FetchedListing> fetchListings(String boardId) {
        String host = normalizeHost(boardId);
        List<FetchedListing> out = new ArrayList<>();
        int offset = 0;
        int page = 1;

        while (true) {
            Map<String, Object> data = http.get()
                .uri("https://{host}/api/jobs?page={page}&limit={limit}",
                    host, page++, PAGE_SIZE)
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .body(Map.class);

            List<Map<String, Object>> jobs = data == null
                ? List.of()
                : (List<Map<String, Object>>) data.getOrDefault("jobs", List.of());
            if (jobs.isEmpty()) break;

            for (Map<String, Object> wrapper : jobs) {
                Map<String, Object> job =
                    wrapper.containsKey("data")
                        ? (Map<String, Object>) wrapper.get("data")
                        : wrapper;

                out.add(new FetchedListing(
                    String.valueOf(job.get("req_id")),
                    (String) job.get("title"),
                    url(host, job),
                    splitLocations((String) job.get("full_location")),
                    ListingStatus.ACTIVE,
                    parseInstant((String) job.get("posted_date")),
                    parseInstant((String) job.get("update_date"))
                ));
            }

            offset += jobs.size();
            Object total = data.get("totalCount");
            if (total == null) total = data.get("count");
            if (total instanceof Number n && offset >= n.longValue()) break;
        }
        return out;
    }

    static String normalizeHost(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Invalid Jibe boardId");
        }
        String s = raw.strip();
        if (s.startsWith("https://")) s = s.substring("https://".length());
        if (s.startsWith("http://")) s = s.substring("http://".length());
        while (s.endsWith("/")) s = s.substring(0, s.length() - 1);
        return s;
    }

    @SuppressWarnings("unchecked")
    private static String url(String host, Map<String, Object> job) {
        Map<String, Object> meta =
            (Map<String, Object>) job.getOrDefault("meta_data", Map.of());
        Object canonical = meta.get("canonical_url");
        if (canonical instanceof String s && !s.isBlank()) return s;

        Object reqId = job.get("req_id");
        String lang = (String) job.getOrDefault("language", "en-us");
        return "https://" + host + "/jobs/" + reqId + "?lang=" + lang;
    }

    private static List<String> splitLocations(String fullLocation) {
        if (fullLocation == null || fullLocation.isBlank()) return List.of();
        return Arrays.stream(fullLocation.split(";"))
            .map(String::strip)
            .filter(s -> !s.isEmpty())
            .toList();
    }

    private static Instant parseInstant(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Instant.parse(s); }
        catch (Exception e) { return null; }
    }
}
