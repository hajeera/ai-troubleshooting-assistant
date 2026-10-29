package com.troubleshooting.assistant.model;

/**
 * An incident returned by a similarity search, with a score from 0.0 (no match) to 1.0 (best match).
 * The score lets the AI (and you) judge how relevant each result is.
 */
public record IncidentMatch(Incident incident, double score) {
}
