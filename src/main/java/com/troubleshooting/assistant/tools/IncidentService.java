package com.troubleshooting.assistant.tools;

import com.troubleshooting.assistant.model.IncidentMatch;
import com.troubleshooting.assistant.rag.IncidentIndexer;
import com.troubleshooting.assistant.repository.MockDataRepository;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * Tool 3: find past incidents similar to a description, by MEANING (semantic search).
 * <p>
 * Day 4 version: the query is turned into an embedding (vector) and pgvector returns the stored
 * incidents whose vectors are closest by cosine similarity. "all db connections busy" now finds
 * INC-1042 ("connection pool exhaustion") even though the words differ.
 * <p>
 * The Day 2 keyword version is kept in test sources as KeywordIncidentSearch, for comparison.
 */
@Service
public class IncidentService {

    static final int DEFAULT_LIMIT = 3;
    static final int MAX_LIMIT = 10;

    private final VectorStore vectorStore;
    private final MockDataRepository repository;
    private final double similarityThreshold;

    public IncidentService(VectorStore vectorStore,
                           MockDataRepository repository,
                           @Value("${app.incidents.similarity-threshold:0.3}") double similarityThreshold) {
        this.vectorStore = vectorStore;
        this.repository = repository;
        this.similarityThreshold = similarityThreshold;
    }

    /**
     * @param query free-text description of the current problem
     * @param limit optional max results; null = 3, capped at 10
     * @return matches above the similarity threshold, best first
     */
    public List<IncidentMatch> findSimilarIncidents(String query, Integer limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }

        SearchRequest request = SearchRequest.builder()
                .query(query)                              // embedded by the EmbeddingModel inside the store
                .topK(effectiveLimit(limit))               // how many nearest neighbours to return
                .similarityThreshold(similarityThreshold)  // drop weak matches instead of returning "closest anyway"
                .build();

        return vectorStore.similaritySearch(request).stream()
                .map(this::toMatch)
                .filter(Objects::nonNull)
                .toList();
    }

    /** A stored Document carries the incident id in its metadata; look up the full incident by it. */
    private IncidentMatch toMatch(Document document) {
        Object id = document.getMetadata().get(IncidentIndexer.INCIDENT_ID);
        if (id == null) {
            return null;
        }
        double score = document.getScore() == null ? 0.0 : document.getScore();
        return repository.findIncident(id.toString())
                .map(incident -> new IncidentMatch(incident, Math.round(score * 1000) / 1000.0))
                .orElse(null);
    }

    private static int effectiveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
