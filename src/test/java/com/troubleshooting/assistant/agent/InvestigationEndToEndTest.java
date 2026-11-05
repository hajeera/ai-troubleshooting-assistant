package com.troubleshooting.assistant.agent;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * END-TO-END: real chat LLM + real tool calls + real RAG (OpenAI + pgvector in Docker).
 * Runs only when OPENAI_API_KEY is set. Takes ~10-60 s and costs a fraction of a rupee.
 * <p>
 * LLM answers vary between runs, so the assertions are deliberately loose: they check the agent
 * found the right evidence, not exact wording. Read the printed output to judge quality.
 */
@SpringBootTest
@EnabledIfEnvironmentVariable(named = "OPENAI_API_KEY", matches = ".+")
class InvestigationEndToEndTest {

    @Autowired
    private InvestigationService service;

    @Test
    void diagnosesThePaymentServiceOutage() {
        AskResponse answer = service.investigate(
                "payment-service is failing. Investigate and tell me the likely root cause, with evidence.");

        print(answer);

        Diagnosis d = answer.diagnosis();
        assertThat(d).isNotNull();
        assertThat(d.evidence()).isNotEmpty();
        assertThat(String.join(" ", d.likelyRootCause(), d.summary()).toLowerCase())
                .containsAnyOf("connection", "pool", "refund");
        assertThat(String.join(" ", d.similarIncidents())).contains("INC-1042");
        assertThat(answer.totalTokens()).isPositive();
    }

    private static void print(AskResponse a) {
        Diagnosis d = a.diagnosis();
        System.out.println("\n===== DIAGNOSIS =====");
        System.out.println("Summary:     " + d.summary());
        System.out.println("Root cause:  " + d.likelyRootCause());
        System.out.println("Confidence:  " + d.confidence());
        System.out.println("Evidence:");
        d.evidence().forEach(e -> System.out.println("  - " + e));
        System.out.println("Similar incidents:");
        d.similarIncidents().forEach(i -> System.out.println("  - " + i));
        System.out.println("Next steps:");
        d.suggestedNextSteps().forEach(s -> System.out.println("  - " + s));
        System.out.printf("Model=%s promptTokens=%s completionTokens=%s totalTokens=%s latencyMs=%d%n",
                a.model(), a.promptTokens(), a.completionTokens(), a.totalTokens(), a.latencyMs());
    }
}
