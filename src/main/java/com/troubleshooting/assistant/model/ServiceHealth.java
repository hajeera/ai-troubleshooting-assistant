package com.troubleshooting.assistant.model;

import java.time.Instant;
import java.util.Map;

/** Current health of one service, as stored in data/services.json. */
public record ServiceHealth(
        String name,
        String version,
        String status,
        Instant lastDeployment,
        Map<String, String> components,
        Map<String, Object> metrics) {
}
