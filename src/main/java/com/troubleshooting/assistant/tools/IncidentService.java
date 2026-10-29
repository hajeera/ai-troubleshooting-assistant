package com.troubleshooting.assistant.tools;

import com.troubleshooting.assistant.model.Incident;
import com.troubleshooting.assistant.model.IncidentMatch;
import com.troubleshooting.assistant.repository.MockDataRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tool 3: find past incidents similar to a description.
 * <p>
 * Day 2 version: simple keyword overlap. It only matches exact words, so "exhausted" does NOT
 * match "exhaustion" and "DB" does not match "database". Day 4 replaces this with embeddings
 * and pgvector, which match by meaning. Keep the results from this version to compare.
 */
@Service
public class IncidentService {

    static final int DEFAULT_LIMIT = 3;
    static final int MAX_LIMIT = 10;

    /** Common words that carry no meaning for matching. */
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "are", "was", "with", "why", "what", "how", "from", "this", "that",
            "service", "is", "a", "an", "of", "to", "in", "on");

    private final MockDataRepository repository;

    public IncidentService(MockDataRepository repository) {
        this.repository = repository;
    }

    /**
     * @param query free-text description of the current problem
     * @param limit optional max results; null = 3, capped at 10
     * @return matches with score > 0, best first
     */
    public List<IncidentMatch> findSimilarIncidents(String query, Integer limit) {
        Set<String> queryWords = words(query);
        if (queryWords.isEmpty()) {
            return List.of();
        }

        return repository.findAllIncidents().stream()
                .map(incident -> new IncidentMatch(incident, score(queryWords, incident)))
                .filter(match -> match.score() > 0)
                .sorted(Comparator.comparingDouble(IncidentMatch::score).reversed()
                        .thenComparing(match -> match.incident().id()))
                .limit(effectiveLimit(limit))
                .toList();
    }

    /** Fraction of the query's words that appear in the incident (0.0 to 1.0). */
    private static double score(Set<String> queryWords, Incident incident) {
        Set<String> incidentWords = words(String.join(" ",
                incident.title(), incident.symptoms(), incident.rootCause(),
                String.join(" ", incident.tags())));
        long hits = queryWords.stream().filter(incidentWords::contains).count();
        return (double) hits / queryWords.size();
    }

    /** Lower-case, split on anything that is not a letter or digit, drop stop words and short words. */
    private static Set<String> words(String text) {
        if (text == null) {
            return Set.of();
        }
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(w -> w.length() >= 3)
                .filter(w -> !STOP_WORDS.contains(w))
                .collect(Collectors.toSet());
    }

    private static int effectiveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }
}
