package com.troubleshooting.assistant.tools;

import com.troubleshooting.assistant.model.LogEntry;
import com.troubleshooting.assistant.repository.MockDataRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Tool 1: search application logs. */
@Service
public class LogSearchService {

    static final int DEFAULT_LIMIT = 20;
    static final int MAX_LIMIT = 50;

    private final MockDataRepository repository;
    private final ServiceHealthService serviceHealthService;

    public LogSearchService(MockDataRepository repository, ServiceHealthService serviceHealthService) {
        this.repository = repository;
        this.serviceHealthService = serviceHealthService;
    }

    /**
     * @param service required, e.g. "payment-service"
     * @param level   optional: ERROR, WARN, INFO or DEBUG (case-insensitive); null = all levels
     * @param keyword optional text to look for in the message (case-insensitive); null = no filter
     * @param limit   optional max number of entries; null = 20, capped at 50
     * @return matching entries, newest first
     */
    public List<LogEntry> searchLogs(String service, String level, String keyword, Integer limit) {
        serviceHealthService.requireKnownService(service);

        String keywordLower = isBlank(keyword) ? null : keyword.toLowerCase(Locale.ROOT);

        return repository.findAllLogs().stream()
                .filter(e -> e.service().equalsIgnoreCase(service))
                .filter(e -> isBlank(level) || e.level().equalsIgnoreCase(level.trim()))
                .filter(e -> keywordLower == null
                        || e.message().toLowerCase(Locale.ROOT).contains(keywordLower))
                .sorted(Comparator.comparing(LogEntry::timestamp).reversed())
                .limit(effectiveLimit(limit))
                .toList();
    }

    private static int effectiveLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
