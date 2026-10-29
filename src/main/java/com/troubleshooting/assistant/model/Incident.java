package com.troubleshooting.assistant.model;

import java.time.LocalDate;
import java.util.List;

/** A past production incident, as stored in data/incidents.json. */
public record Incident(
        String id,
        String title,
        String service,
        String severity,
        LocalDate date,
        String symptoms,
        String rootCause,
        String resolution,
        List<String> tags) {
}
