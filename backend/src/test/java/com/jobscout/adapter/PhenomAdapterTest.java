package com.jobscout.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class PhenomAdapterTest {

    @Test
    void readsLocationNamesFromMultiLocationArrayObjects() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://jobs.acme.com/widgets"))
            .andRespond(withSuccess("""
                {"refineSearch":{"totalHits":1,"data":{"jobs":[{
                  "jobId":"R-1","title":"Analyst",
                  "location":"Toronto, Ontario, Canada",
                  "multi_location":["Toronto, Ontario, Canada","Montreal, Quebec, Canada"],
                  "multi_location_array":[
                    {"latlong":{"lon":-79.38,"lat":43.65},"location":"Toronto, Ontario, Canada"},
                    {"latlong":{"lon":-73.58,"lat":45.50},"location":"Montreal, Quebec, Canada"}
                  ]}]}}}""", MediaType.APPLICATION_JSON));

        List<FetchedListing> listings =
            new PhenomAdapter(builder.build()).fetchListings("jobs.acme.com/ca/en:ACME01");

        assertThat(listings).singleElement()
            .extracting(FetchedListing::locations)
            .isEqualTo(List.of("Toronto, Ontario, Canada", "Montreal, Quebec, Canada"));
    }
}
