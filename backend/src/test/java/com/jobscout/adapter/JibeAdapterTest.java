package com.jobscout.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.hamcrest.Matchers.startsWith;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class JibeAdapterTest {

    @Test
    void pagesWithPageAndLimitParameters() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        expectPage(server, 1, 0, 100, 150);
        expectPage(server, 2, 100, 50, 150);

        List<FetchedListing> listings =
            new JibeAdapter(builder.build()).fetchListings("jobs.acme.com");

        assertThat(listings).extracting(FetchedListing::externalId)
            .doesNotHaveDuplicates()
            .hasSize(150);
        server.verify();
    }

    private static void expectPage(MockRestServiceServer server, int page,
                                   int firstId, int count, int total) {
        String jobs = IntStream.range(firstId, firstId + count)
            .mapToObj(i -> """
                {"data":{"req_id":"%d","title":"Job %d","full_location":"Toronto"}}"""
                .formatted(i, i))
            .collect(Collectors.joining(","));
        server.expect(requestTo(startsWith("https://jobs.acme.com/api/jobs")))
            .andExpect(queryParam("page", String.valueOf(page)))
            .andExpect(queryParam("limit", "100"))
            .andRespond(withSuccess(
                "{\"totalCount\":%d,\"jobs\":[%s]}".formatted(total, jobs),
                MediaType.APPLICATION_JSON));
    }
}
