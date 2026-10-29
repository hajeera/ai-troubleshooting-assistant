package com.troubleshooting.assistant.tools;

import com.troubleshooting.assistant.model.ServiceHealth;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ServiceHealthServiceTest {

    private ServiceHealthService health;

    @BeforeEach
    void setUp() {
        health = new ServiceHealthService(TestData.repository());
    }

    @Test
    void listsAllServicesSorted() {
        assertThat(health.listServices())
                .containsExactly("inventory-service", "order-service", "payment-service");
    }

    @Test
    void paymentServiceIsDownBecauseOfDatabase() {
        ServiceHealth payment = health.checkServiceHealth("payment-service");

        assertThat(payment.status()).isEqualTo("DOWN");
        assertThat(payment.components()).containsEntry("db", "DOWN");
        assertThat(payment.metrics()).containsKey("dbPoolWaiting");
    }

    @Test
    void inventoryServiceIsUp() {
        assertThat(health.checkServiceHealth("inventory-service").status()).isEqualTo("UP");
    }

    @Test
    void unknownServiceGivesHelpfulError() {
        assertThatThrownBy(() -> health.checkServiceHealth("nope"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Known services");
    }
}
