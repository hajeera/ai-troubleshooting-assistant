package com.troubleshooting.assistant.model;

import java.time.Instant;

/**
 * One parsed log line, e.g.
 * 2026-10-29T09:06:30.002Z ERROR [payment-service,http-nio-8080-exec-9] o.h.e.j.s.SqlExceptionHelper : Connection is not available
 */
public record LogEntry(
        Instant timestamp,
        String level,
        String service,
        String thread,
        String logger,
        String message) {
}
