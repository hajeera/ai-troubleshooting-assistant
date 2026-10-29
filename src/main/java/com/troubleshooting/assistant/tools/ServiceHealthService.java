package com.troubleshooting.assistant.tools;

import com.troubleshooting.assistant.model.ServiceHealth;
import com.troubleshooting.assistant.repository.MockDataRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/** Tool 2: service health and the list of known services. */
@Service
public class ServiceHealthService {

    private final MockDataRepository repository;

    public ServiceHealthService(MockDataRepository repository) {
        this.repository = repository;
    }

    /** Names of all services the assistant knows about, sorted. */
    public List<String> listServices() {
        return repository.findAllServices().stream()
                .map(ServiceHealth::name)
                .sorted()
                .toList();
    }

    /** Status, component health and key metrics for one service. */
    public ServiceHealth checkServiceHealth(String service) {
        requireKnownService(service);
        return repository.findService(service).orElseThrow();
    }

    /**
     * Throws a clear error for unknown names. The message lists valid names so that an AI
     * reading it can correct itself and retry, instead of guessing.
     */
    void requireKnownService(String service) {
        if (service == null || repository.findService(service.trim()).isEmpty()) {
            throw new IllegalArgumentException(
                    "Unknown service '" + service + "'. Known services: " + listServices());
        }
    }
}
