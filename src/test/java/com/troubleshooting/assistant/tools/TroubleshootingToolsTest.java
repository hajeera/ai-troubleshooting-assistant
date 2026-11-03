package com.troubleshooting.assistant.tools;

import com.troubleshooting.assistant.config.ToolConfig;
import com.troubleshooting.assistant.repository.MockDataRepository;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.vectorstore.VectorStore;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/** Checks what the AI will see: tool names, descriptions and parameter schemas, plus one real call. */
class TroubleshootingToolsTest {

    private final ToolCallbackProvider provider = new ToolConfig().troubleshootingToolCallbacks(tools());

    private static TroubleshootingTools tools() {
        MockDataRepository repository = TestData.repository();
        ServiceHealthService health = new ServiceHealthService(repository);
        return new TroubleshootingTools(health,
                new LogSearchService(repository, health),
                new IncidentService(mock(VectorStore.class), repository, 0.3));   // vector search not used in these tests
    }

    @Test
    void exposesFourToolsByName() {
        assertThat(Arrays.stream(provider.getToolCallbacks()).map(t -> t.getToolDefinition().name()))
                .containsExactlyInAnyOrder("listServices", "checkServiceHealth", "searchLogs", "findSimilarIncidents");
    }

    @Test
    void searchLogsSchemaDescribesItsParameters() {
        ToolCallback searchLogs = callback("searchLogs");

        // The JSON schema the model receives. Print it to see exactly what Claude reads.
        String schema = searchLogs.getToolDefinition().inputSchema();
        System.out.println(schema);

        assertThat(schema).contains("service", "level", "keyword", "limit");
    }

    @Test
    void callingAToolWithJsonArgumentsWorks() {
        // This is what happens when the model calls the tool: arguments arrive as JSON text.
        String result = callback("checkServiceHealth").call("{\"service\":\"payment-service\"}");

        assertThat(result).contains("DOWN");
    }

    private ToolCallback callback(String name) {
        return Arrays.stream(provider.getToolCallbacks())
                .filter(t -> t.getToolDefinition().name().equals(name))
                .findFirst()
                .orElseThrow();
    }
}
