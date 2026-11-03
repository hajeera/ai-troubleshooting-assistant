package com.troubleshooting.assistant.rag;

import com.troubleshooting.assistant.model.Incident;
import com.troubleshooting.assistant.repository.MockDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * INGESTION (the "indexing" phase of RAG): runs once after the app starts.
 * <p>
 * For each incident: build a text → the VectorStore calls the embedding model to turn it into a
 * vector → stores text + vector + metadata in the pgvector table "vector_store".
 * <p>
 * Embedding costs time and money, so it is skipped when all incidents are already stored
 * (the Docker volume keeps the table across restarts).
 */
@Component
public class IncidentIndexer implements ApplicationRunner {

    /** Metadata key holding the incident id, e.g. "INC-1042". Used to map search results back to incidents. */
    public static final String INCIDENT_ID = "incidentId";

    private static final Logger log = LoggerFactory.getLogger(IncidentIndexer.class);

    private final MockDataRepository repository;
    private final VectorStore vectorStore;
    private final JdbcTemplate jdbc;
    private final boolean indexOnStartup;

    public IncidentIndexer(MockDataRepository repository,
                           VectorStore vectorStore,
                           JdbcTemplate jdbc,
                           @Value("${app.incidents.index-on-startup:true}") boolean indexOnStartup) {
        this.repository = repository;
        this.vectorStore = vectorStore;
        this.jdbc = jdbc;
        this.indexOnStartup = indexOnStartup;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!indexOnStartup) {
            log.info("Incident indexing disabled (app.incidents.index-on-startup=false)");
            return;
        }

        List<Incident> incidents = repository.findAllIncidents();
        Integer stored = jdbc.queryForObject("SELECT count(*) FROM vector_store", Integer.class);

        if (stored != null && stored >= incidents.size()) {
            log.info("Incidents already indexed ({} rows in vector_store), skipping embedding", stored);
            return;
        }

        long start = System.currentTimeMillis();
        List<Document> documents = incidents.stream().map(IncidentIndexer::toDocument).toList();
        vectorStore.add(documents);   // embeds every document, then inserts (same id = update, not duplicate)
        log.info("Indexed {} incidents into pgvector in {} ms", documents.size(), System.currentTimeMillis() - start);
    }

    /**
     * What gets embedded: the parts that describe the PROBLEM (title, symptoms, cause, tags), because
     * queries describe a problem. The resolution is still available from the full incident record.
     */
    static Document toDocument(Incident incident) {
        String text = """
                Title: %s
                Service: %s
                Symptoms: %s
                Root cause: %s
                Tags: %s""".formatted(
                incident.title(), incident.service(), incident.symptoms(),
                incident.rootCause(), String.join(", ", incident.tags()));

        Map<String, Object> metadata = Map.of(
                INCIDENT_ID, incident.id(),
                "service", incident.service(),
                "severity", incident.severity());

        return new Document(stableId(incident.id()), text, metadata);
    }

    /**
     * pgvector's id column is a UUID. Deriving it from the incident id means the same incident always
     * gets the same UUID, so re-indexing updates rows instead of creating duplicates.
     */
    public static String stableId(String incidentId) {
        return UUID.nameUUIDFromBytes(incidentId.getBytes(StandardCharsets.UTF_8)).toString();
    }
}
