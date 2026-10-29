package com.troubleshooting.assistant.tools;

import com.troubleshooting.assistant.model.LogEntry;
import com.troubleshooting.assistant.repository.MockDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Comparator;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogSearchServiceTest {

    private LogSearchService logSearch;

    @BeforeEach
    void setUp() {
        MockDataRepository repository = TestData.repository();
        logSearch = new LogSearchService(repository, new ServiceHealthService(repository));
    }

    @Test
    void findsPaymentErrorsAboutConnections() {
        List<LogEntry> result = logSearch.searchLogs("payment-service", "ERROR", "connection", null);

        assertThat(result).isNotEmpty();
        assertThat(result).allSatisfy(e -> {
            assertThat(e.service()).isEqualTo("payment-service");
            assertThat(e.level()).isEqualTo("ERROR");
            assertThat(e.message()).containsIgnoringCase("connection");
        });
    }

    @Test
    void returnsNewestFirst() {
        List<LogEntry> result = logSearch.searchLogs("order-service", null, null, null);

        assertThat(result).isSortedAccordingTo(Comparator.comparing(LogEntry::timestamp).reversed());
    }

    @Test
    void levelFilterIsCaseInsensitive() {
        assertThat(logSearch.searchLogs("order-service", "warn", null, null))
                .isNotEmpty()
                .allSatisfy(e -> assertThat(e.level()).isEqualTo("WARN"));
    }

    @Test
    void respectsLimit() {
        assertThat(logSearch.searchLogs("payment-service", null, null, 2)).hasSize(2);
    }

    @Test
    void healthyServiceHasNoErrors() {
        assertThat(logSearch.searchLogs("inventory-service", "ERROR", null, null)).isEmpty();
    }

    @Test
    void unknownServiceGivesHelpfulError() {
        assertThatThrownBy(() -> logSearch.searchLogs("billing-service", null, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("billing-service")
                .hasMessageContaining("payment-service");
    }
}
