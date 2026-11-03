package com.troubleshooting.assistant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Starts the whole Spring application: checks all beans can be created and wired.
 * Needs the Postgres container running (docker compose up -d).
 * Does NOT call OpenAI: indexing is off and a dummy key is used, so it runs without a real key.
 */
@SpringBootTest(properties = {
        "app.incidents.index-on-startup=false",
        "spring.ai.openai.api-key=dummy-key-not-used"
})
class TroubleshootingAssistantApplicationTests {

    @Test
    void contextLoads() {
        // Passes if the Spring application context starts without errors
    }
}
