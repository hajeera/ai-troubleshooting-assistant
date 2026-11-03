package com.troubleshooting.assistant.rag;

import com.troubleshooting.assistant.model.IncidentMatch;
import com.troubleshooting.assistant.tools.IncidentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * INTEGRATION test: real OpenAI embeddings + real pgvector (Docker).
 * Runs only when the OPENAI_API_KEY environment variable is set; otherwise it is skipped.
 * Costs a fraction of a rupee per run (a few short query embeddings; incidents are embedded once).
 * <p>
 * Prints every query's matches and scores: use them to understand and tune the similarity threshold.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class IncidentSemanticSearchTest {

    @Autowired
    private IncidentService incidents;

    @Test
    void findsTheRefundJobIncidentEvenWithDifferentWords() {
        // The query that keyword search MISSED (see KeywordIncidentSearchTest.knownLimitationExactWordsOnly)
        List<IncidentMatch> result = search("all db connections busy, payments failing");

        assertThat(result).extracting(m -> m.incident().id()).contains("INC-1042");
    }

    @Test
    void findsDnsIncidentFromAParaphrase() {
        List<IncidentMatch> result = search("services cannot resolve hostnames, UnknownHostException everywhere");

        assertThat(result).isNotEmpty();
        assertThat(result.getFirst().incident().id()).isEqualTo("INC-1140");
    }

    @Test
    void findsMemoryIncidentsForOutOfMemoryQuery() {
        List<IncidentMatch> result = search("pods keep restarting with OutOfMemoryError");

        assertThat(result).extracting(m -> m.incident().id()).containsAnyOf("INC-1063", "INC-1169");
    }

    @Test
    void scoresAreBestFirst() {
        List<IncidentMatch> result = search("kafka consumers falling behind", 10);

        assertThat(result).extracting(IncidentMatch::score)
                .isSortedAccordingTo((a, b) -> Double.compare(b, a));
    }

    private List<IncidentMatch> search(String query) {
        return search(query, 3);
    }

    private List<IncidentMatch> search(String query, int limit) {
        List<IncidentMatch> result = incidents.findSimilarIncidents(query, limit);
        System.out.println("\nQUERY: " + query);
        result.forEach(m -> System.out.printf("  %.3f  %s  %s%n", m.score(), m.incident().id(), m.incident().title()));
        return result;
    }
}
