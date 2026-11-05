package com.troubleshooting.assistant.tools;

import com.troubleshooting.assistant.model.IncidentMatch;
import com.troubleshooting.assistant.model.LogEntry;
import com.troubleshooting.assistant.model.ServiceHealth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * The AI-facing layer: the methods an LLM is allowed to call.
 * <p>
 * Each method only delegates to a Day 2 service. The value added here is the descriptions:
 * the model reads them to decide WHICH tool to call and WHAT arguments to pass, so they are
 * written like instructions to a new on-call engineer.
 * <p>
 * Every call is logged (tool, arguments, result size, duration). In the "mcp" profile the log goes
 * to a file, never to stdout, so the MCP protocol stream stays clean.
 * <p>
 * Errors: an unknown service throws IllegalArgumentException whose message lists valid names.
 * Spring AI returns that message to the model as the tool result, so it can retry correctly.
 */
@Component
public class TroubleshootingTools {

    private static final Logger log = LoggerFactory.getLogger(TroubleshootingTools.class);

    private final ServiceHealthService health;
    private final LogSearchService logSearch;
    private final IncidentService incidents;

    public TroubleshootingTools(ServiceHealthService health, LogSearchService logSearch, IncidentService incidents) {
        this.health = health;
        this.logSearch = logSearch;
        this.incidents = incidents;
    }

    @Tool(name = "listServices",
            description = "List the names of all services that can be investigated. "
                    + "Call this first if you are not sure of the exact service name.")
    public List<String> listServices() {
        return logged("listServices", "", health::listServices);
    }

    @Tool(name = "checkServiceHealth",
            description = "Get the current health of one service: overall status (UP, DEGRADED, DOWN), "
                    + "health of its components (db, kafka, circuit breakers) and key metrics "
                    + "(error rate, p99 latency, DB pool usage, CPU, heap). "
                    + "Start an investigation here.")
    public ServiceHealth checkServiceHealth(
            @ToolParam(description = "Exact service name, e.g. payment-service") String service) {
        return logged("checkServiceHealth", "service=" + service,
                () -> health.checkServiceHealth(service));
    }

    @Tool(name = "searchLogs",
            description = "Search a service's application logs, newest first. "
                    + "Use level=ERROR or WARN to find failures, and keyword to narrow down "
                    + "(e.g. 'timeout', 'connection', 'circuit'). Returns at most 50 entries.")
    public List<LogEntry> searchLogs(
            @ToolParam(description = "Exact service name, e.g. payment-service") String service,
            @ToolParam(description = "Optional log level: ERROR, WARN, INFO or DEBUG", required = false) String level,
            @ToolParam(description = "Optional case-insensitive text to find in the message", required = false) String keyword,
            @ToolParam(description = "Optional max entries to return, default 20, max 50", required = false) Integer limit) {
        return logged("searchLogs",
                "service=%s level=%s keyword=%s limit=%s".formatted(service, level, keyword, limit),
                () -> logSearch.searchLogs(service, level, keyword, limit));
    }

    @Tool(name = "findSimilarIncidents",
            description = "Find past production incidents similar to the current problem, with their "
                    + "root cause and resolution. Describe the symptoms you observed "
                    + "(e.g. 'payment requests fail with DB connection pool exhausted'). "
                    + "Each result has a similarity score from 0 to 1; treat lower scores as weaker evidence.")
    public List<IncidentMatch> findSimilarIncidents(
            @ToolParam(description = "Description of the symptoms or suspected cause") String query,
            @ToolParam(description = "Optional max results, default 3, max 10", required = false) Integer limit) {
        return logged("findSimilarIncidents", "query=\"%s\" limit=%s".formatted(query, limit),
                () -> incidents.findSimilarIncidents(query, limit));
    }

    /**
     * Runs one tool call and logs it: name, arguments, how many results, how long it took.
     * Failures are logged at WARN and re-thrown, so the model still receives the error message.
     */
    private <T> T logged(String tool, String args, Supplier<T> call) {
        long start = System.nanoTime();
        try {
            T result = call.get();
            log.info("tool={} {} results={} durationMs={}", tool, args, sizeOf(result), elapsedMs(start));
            return result;
        } catch (RuntimeException e) {
            log.warn("tool={} {} failed after {} ms: {}", tool, args, elapsedMs(start), e.getMessage());
            throw e;
        }
    }

    private static int sizeOf(Object result) {
        return result instanceof Collection<?> c ? c.size() : (result == null ? 0 : 1);
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }
}
