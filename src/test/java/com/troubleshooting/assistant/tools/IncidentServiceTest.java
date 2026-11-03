package com.troubleshooting.assistant.tools;

import com.troubleshooting.assistant.model.IncidentMatch;
import com.troubleshooting.assistant.rag.IncidentIndexer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Fast unit tests for the semantic IncidentService, WITHOUT OpenAI or Postgres:
 * the VectorStore is replaced by a Mockito mock that returns prepared Documents.
 * We test OUR logic: the search request we build and how results are mapped back to incidents.
 * (The real embedding + pgvector behaviour is covered by IncidentSemanticSearchTest.)
 */
class IncidentServiceTest {

    private VectorStore vectorStore;
    private IncidentService incidents;

    @BeforeEach
    void setUp() {
        vectorStore = mock(VectorStore.class);
        incidents = new IncidentService(vectorStore, TestData.repository(), 0.3);
    }

    @Test
    void buildsSearchRequestWithQueryTopKAndThreshold() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());

        incidents.findSimilarIncidents("all db connections busy", 5);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        assertThat(captor.getValue().getQuery()).isEqualTo("all db connections busy");
        assertThat(captor.getValue().getTopK()).isEqualTo(5);
        assertThat(captor.getValue().getSimilarityThreshold()).isEqualTo(0.3);
    }

    @Test
    void defaultsAndCapsTheLimit() {
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of());
        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);

        incidents.findSimilarIncidents("x", null);
        incidents.findSimilarIncidents("x", 500);

        verify(vectorStore, times(2)).similaritySearch(captor.capture());
        assertThat(captor.getAllValues()).extracting(SearchRequest::getTopK).containsExactly(3, 10);
    }

    @Test
    void mapsDocumentsBackToFullIncidentsWithScores() {
        Document hit = Document.builder()
                .id(IncidentIndexer.stableId("INC-1042"))
                .text("Title: Payment failures due to DB connection pool exhaustion ...")
                .metadata(Map.of(IncidentIndexer.INCIDENT_ID, "INC-1042"))
                .score(0.8123)
                .build();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(hit));

        List<IncidentMatch> result = incidents.findSimilarIncidents("all db connections busy", null);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().incident().id()).isEqualTo("INC-1042");
        assertThat(result.getFirst().incident().resolution()).contains("chunked commits"); // full record, not just the embedded text
        assertThat(result.getFirst().score()).isEqualTo(0.812);
    }

    @Test
    void ignoresDocumentsWithUnknownIncidentId() {
        Document stale = Document.builder()
                .id(IncidentIndexer.stableId("INC-9999"))
                .text("deleted incident")
                .metadata(Map.of(IncidentIndexer.INCIDENT_ID, "INC-9999"))
                .score(0.9)
                .build();
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(List.of(stale));

        assertThat(incidents.findSimilarIncidents("anything", null)).isEmpty();
    }

    @Test
    void blankQueryReturnsNothingWithoutCallingTheStore() {
        assertThat(incidents.findSimilarIncidents("  ", null)).isEmpty();
        verifyNoInteractions(vectorStore);
    }
}
