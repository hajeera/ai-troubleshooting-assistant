package com.troubleshooting.assistant.agent;

import java.util.List;

/**
 * Structured answer the LLM must return. Spring AI derives a JSON schema from this record,
 * adds it to the prompt as format instructions, and converts the model's JSON reply into it.
 * Structured output means other systems (a dashboard, a Slack bot) can use the result, not just humans.
 */
public record Diagnosis(
        String summary,                   // one or two sentences: what is wrong
        String likelyRootCause,           // the most likely cause, or "insufficient evidence"
        String confidence,                // LOW, MEDIUM or HIGH
        List<String> evidence,            // facts from tool results: metrics, log lines with timestamps
        List<String> similarIncidents,    // e.g. "INC-1042 (score 0.585): refund job held DB connections"
        List<String> suggestedNextSteps) {
}
