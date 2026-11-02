package com.troubleshooting.assistant.tools;

import com.troubleshooting.assistant.model.IncidentMatch;
import com.troubleshooting.assistant.model.LogEntry;
import com.troubleshooting.assistant.model.ServiceHealth;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * The AI-facing layer: the methods an LLM is allowed to call.
 * <p>
 * Each method only delegates to a Day 2 service. The value added here is the descriptions:
 * the model reads them to decide WHICH tool to call and WHAT arguments to pass, so they are
 * written like instructions to a new on-call engineer.
 * <p>
 * The same class serves both entry points: the MCP server (Day 3, Claude Desktop) and
 * the /ask endpoint (Day 5, our own ChatClient).
 * <p>
 * Errors: an unknown service throws IllegalArgumentException whose message lists valid names.
 * Spring AI returns that message to the model as the tool result, so it can retry correctly.
 */
@Component
public class TroubleshootingTools {

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
        return health.listServices();
    }

    @Tool(name = "checkServiceHealth",
            description = "Get the current health of one service: overall status (UP, DEGRADED, DOWN), "
                    + "health of its components (db, kafka, circuit breakers) and key metrics "
                    + "(error rate, p99 latency, DB pool usage, CPU, heap). "
                    + "Start an investigation here.")
    public ServiceHealth checkServiceHealth(
            @ToolParam(description = "Exact service name, e.g. payment-service") String service) {
        return health.checkServiceHealth(service);
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
        return logSearch.searchLogs(service, level, keyword, limit);
    }

    @Tool(name = "findSimilarIncidents",
            description = "Find past production incidents similar to the current problem, with their "
                    + "root cause and resolution. Describe the symptoms you observed "
                    + "(e.g. 'payment requests fail with DB connection pool exhausted'). "
                    + "Each result has a score from 0 to 1; treat low scores as weak evidence.")
    public List<IncidentMatch> findSimilarIncidents(
            @ToolParam(description = "Description of the symptoms or suspected cause") String query,
            @ToolParam(description = "Optional max results, default 3, max 10", required = false) Integer limit) {
        return incidents.findSimilarIncidents(query, limit);
    }
}
