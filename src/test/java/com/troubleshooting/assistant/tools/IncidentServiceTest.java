package com.troubleshooting.assistant.tools;

import com.troubleshooting.assistant.model.IncidentMatch;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentServiceTest {

    private IncidentService incidents;

    @BeforeEach
    void setUp() {
        incidents = new IncidentService(TestData.repository());
    }

    @Test
    void connectionPoolProblemFindsTheRefundJobIncidentFirst() {
        List<IncidentMatch> result = incidents.findSimilarIncidents("connection pool exhausted", null);

        assertThat(result).isNotEmpty().hasSizeLessThanOrEqualTo(3);
        assertThat(result.getFirst().incident().id()).isEqualTo("INC-1042");
    }

    @Test
    void dnsProblemFindsDnsIncident() {
        List<IncidentMatch> result = incidents.findSimilarIncidents("DNS resolution", null);

        assertThat(result.getFirst().incident().id()).isEqualTo("INC-1140");
        assertThat(result.getFirst().score()).isEqualTo(1.0);
    }

    @Test
    void resultsAreBestFirst() {
        List<IncidentMatch> result = incidents.findSimilarIncidents("payment database timeout", 10);

        assertThat(result).extracting(IncidentMatch::score)
                .isSortedAccordingTo((a, b) -> Double.compare(b, a));
    }

    @Test
    void unrelatedQueryReturnsNothing() {
        assertThat(incidents.findSimilarIncidents("xyzzy plugh", null)).isEmpty();
    }

    @Test
    void knownLimitationExactWordsOnly() {
        // Same meaning as INC-1042, different words ("connections" vs "connection", "busy" vs "exhaustion"),
        // so keyword matching misses it completely. Day 4 (embeddings) should find it.
        // This test documents the limitation; flip it on Day 4.
        List<IncidentMatch> result = incidents.findSimilarIncidents("all db connections busy", null);

        assertThat(result).noneMatch(m -> m.incident().id().equals("INC-1042"));
    }
}
