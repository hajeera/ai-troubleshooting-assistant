package com.troubleshooting.assistant.repository;

import com.troubleshooting.assistant.model.Incident;
import com.troubleshooting.assistant.model.LogEntry;
import com.troubleshooting.assistant.model.ServiceHealth;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.json.JsonMapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Loads the mock data from src/main/resources/data once, at startup.
 * <p>
 * In a real system this class would be replaced by clients for a log platform (Elasticsearch, Loki),
 * a metrics/health API and an incident database. The tool services only depend on these methods,
 * so they would not change.
 */
@Repository
public class MockDataRepository {

    /**
     * Matches: TIMESTAMP LEVEL [service,thread] logger : message
     * Groups:  1         2     3       4       5        6
     */
    private static final Pattern LOG_LINE = Pattern.compile(
            "^(\\S+)\\s+(\\w+)\\s+\\[([^,\\]]+),([^\\]]+)\\]\\s+(\\S+)\\s+:\\s+(.*)$");

    private final List<LogEntry> logs;
    private final List<ServiceHealth> services;
    private final List<Incident> incidents;

    public MockDataRepository(JsonMapper jsonMapper) {
        this.services = List.of(readJson(jsonMapper, "data/services.json", ServiceHealth[].class));
        this.incidents = List.of(readJson(jsonMapper, "data/incidents.json", Incident[].class));
        this.logs = List.copyOf(readLogs("classpath:data/logs/*.log"));
    }

    public List<LogEntry> findAllLogs() {
        return logs;
    }

    public List<ServiceHealth> findAllServices() {
        return services;
    }

    public Optional<ServiceHealth> findService(String name) {
        return services.stream().filter(s -> s.name().equalsIgnoreCase(name)).findFirst();
    }

    public List<Incident> findAllIncidents() {
        return incidents;
    }

    public Optional<Incident> findIncident(String id) {
        return incidents.stream().filter(i -> i.id().equals(id)).findFirst();
    }

    // ---------- loading helpers ----------

    private static <T> T readJson(JsonMapper mapper, String path, Class<T> type) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return mapper.readValue(in, type);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + path, e);
        }
    }

    private static List<LogEntry> readLogs(String pattern) {
        List<LogEntry> entries = new ArrayList<>();
        try {
            Resource[] files = new PathMatchingResourcePatternResolver().getResources(pattern);
            for (Resource file : files) {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
                    reader.lines()
                            .map(LOG_LINE::matcher)
                            .filter(Matcher::matches)   // skip blank or malformed lines
                            .map(m -> new LogEntry(
                                    Instant.parse(m.group(1)),
                                    m.group(2),
                                    m.group(3).trim(),
                                    m.group(4).trim(),
                                    m.group(5),
                                    m.group(6)))
                            .forEach(entries::add);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read log files " + pattern, e);
        }
        return entries;
    }
}
