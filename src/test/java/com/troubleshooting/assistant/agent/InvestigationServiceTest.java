package com.troubleshooting.assistant.agent;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

/** Input validation, without calling the LLM: bad questions are rejected before any tokens are spent. */
class InvestigationServiceTest {

    private final ChatClient chatClient = mock(ChatClient.class);
    private final InvestigationService service = new InvestigationService(chatClient);

    @Test
    void rejectsEmptyQuestion() {
        assertThatThrownBy(() -> service.investigate("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
        verifyNoInteractions(chatClient);
    }

    @Test
    void rejectsTooLongQuestion() {
        assertThatThrownBy(() -> service.investigate("x".repeat(InvestigationService.MAX_QUESTION_LENGTH + 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at most");
        verifyNoInteractions(chatClient);
    }
}
