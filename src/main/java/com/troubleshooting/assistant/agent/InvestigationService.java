package com.troubleshooting.assistant.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.ResponseEntity;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

/**
 * Runs one investigation: question in, structured Diagnosis out.
 * <p>
 * This is where OUR code drives the LLM (unlike Day 3, where Claude Desktop did).
 * One call to chatClient...call() hides a loop of several round trips to the model:
 * <pre>
 *   question + system prompt + 4 tool definitions  ->  model
 *   model: "call checkServiceHealth(payment-service)"  ->  Spring AI runs TroubleshootingTools
 *   tool result  ->  model: "call searchLogs(...)"  ->  ...  ->  model: final JSON answer
 * </pre>
 */
@Service
public class InvestigationService {

    static final int MAX_QUESTION_LENGTH = 1000;

    private static final Logger log = LoggerFactory.getLogger(InvestigationService.class);

    private final ChatClient chatClient;

    public InvestigationService(ChatClient troubleshootingChatClient) {
        this.chatClient = troubleshootingChatClient;
    }

    public AskResponse investigate(String question) {
        if (question == null || question.isBlank()) {
            throw new IllegalArgumentException("question must not be empty");
        }
        if (question.length() > MAX_QUESTION_LENGTH) {
            throw new IllegalArgumentException("question must be at most " + MAX_QUESTION_LENGTH + " characters");
        }

        long start = System.currentTimeMillis();

        ResponseEntity<ChatResponse, Diagnosis> result = chatClient.prompt()
                .user(question)
                .call()
                .responseEntity(Diagnosis.class);   // tool loop runs here; final reply parsed into Diagnosis

        long latencyMs = System.currentTimeMillis() - start;
        ChatResponse response = result.response();
        Usage usage = response == null ? null : response.getMetadata().getUsage();
        String model = response == null ? null : response.getMetadata().getModel();

        AskResponse answer = new AskResponse(
                result.entity(),
                model,
                usage == null ? null : usage.getPromptTokens(),
                usage == null ? null : usage.getCompletionTokens(),
                usage == null ? null : usage.getTotalTokens(),
                latencyMs);

        log.info("ask model={} promptTokens={} completionTokens={} totalTokens={} latencyMs={} question=\"{}\"",
                answer.model(), answer.promptTokens(), answer.completionTokens(), answer.totalTokens(),
                latencyMs, question);
        return answer;
    }
}
