package com.troubleshooting.assistant.tools;

import com.troubleshooting.assistant.repository.MockDataRepository;
import tools.jackson.databind.json.JsonMapper;

/** Builds the real repository over the real mock files, without starting Spring (fast tests). */
final class TestData {

    private TestData() {
    }

    static MockDataRepository repository() {
        return new MockDataRepository(JsonMapper.builder().build());
    }
}
