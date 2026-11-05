package com.troubleshooting.assistant.agent;

/**
 * What /ask returns: the diagnosis plus how much it cost and how long it took.
 * Token counts and latency are what you watch to control LLM cost in production.
 */
public record AskResponse(
        Diagnosis diagnosis,
        String model,
        Integer promptTokens,
        Integer completionTokens,
        Integer totalTokens,
        long latencyMs) {
}
