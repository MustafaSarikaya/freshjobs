package com.jobscout.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class WorkdayAdapterTest {

    private static final String LIST_URL =
        "https://acme.wd3.myworkdayjobs.com/wday/cxs/acme/Acme_Careers/jobs";

    private MockRestServiceServer listServer;
    private WorkdayAdapter adapter;

    @BeforeEach
    void setUp() {
        RestClient.Builder listBuilder = RestClient.builder();
        listServer = MockRestServiceServer.bindTo(listBuilder).build();
        RestClient.Builder detailBuilder = RestClient.builder();
        MockRestServiceServer.bindTo(detailBuilder).build();
        adapter = new WorkdayAdapter(listBuilder.build(), detailBuilder.build());
    }

    /**
     * Workday only reports the real {@code total} on the first page; later
     * pages come back with {@code total: 0}. Treating that 0 as authoritative
     * stopped every Workday source at 40 listings.
     */
    @Test
    void paginatesPastSecondPageWhenLaterPagesReportZeroTotal() {
        expectPage(0, 45, 0, 20);
        expectPage(20, 0, 20, 20);
        expectPage(40, 0, 40, 5);

        List<FetchedListing> listings = adapter.fetchListings("acme:wd3:Acme_Careers");

        assertThat(listings).hasSize(45);
        listServer.verify();
    }

    /** Workday job URLs must include the site segment or they 404. */
    @Test
    void jobUrlIncludesSiteSegment() {
        expectPage(0, 1, 0, 1);

        List<FetchedListing> listings = adapter.fetchListings("acme:wd3:Acme_Careers");

        assertThat(listings).singleElement()
            .extracting(FetchedListing::url)
            .isEqualTo("https://acme.wd3.myworkdayjobs.com/Acme_Careers/job/Toronto/Job-0_R-0");
    }

    private void expectPage(int offset, int total, int firstIndex, int count) {
        String postings = IntStream.range(firstIndex, firstIndex + count)
            .mapToObj(i -> """
                {"title":"Job %d","externalPath":"/job/Toronto/Job-%d_R-%d",
                 "locationsText":"Toronto","bulletFields":["R-%d"]}"""
                .formatted(i, i, i, i))
            .collect(Collectors.joining(","));
        listServer.expect(requestTo(LIST_URL))
            .andExpect(method(HttpMethod.POST))
            .andExpect(jsonPath("$.offset").value(offset))
            .andRespond(withSuccess(
                "{\"total\":%d,\"jobPostings\":[%s]}".formatted(total, postings),
                MediaType.APPLICATION_JSON));
    }
}
