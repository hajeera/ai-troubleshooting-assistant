package com.troubleshooting.assistant.config;

import com.troubleshooting.assistant.tools.TroubleshootingTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ToolConfig {

    /**
     * Turns every @Tool method in TroubleshootingTools into a "tool callback": a name, a description,
     * a JSON schema of the parameters (built from @ToolParam) and a way to invoke the method.
     * The MCP server auto-configuration finds this bean and publishes the tools in tools/list.
     */
    @Bean
    public ToolCallbackProvider troubleshootingToolCallbacks(TroubleshootingTools tools) {
        return MethodToolCallbackProvider.builder().toolObjects(tools).build();
    }
}
