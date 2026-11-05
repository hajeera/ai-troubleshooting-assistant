package com.troubleshooting.assistant.agent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * POST /ask  {"question": "Why is payment-service failing?"}  ->  AskResponse (diagnosis + tokens + latency)
 */
@RestController
public class AskController {

    private static final Logger log = LoggerFactory.getLogger(AskController.class);

    private final InvestigationService investigationService;

    public AskController(InvestigationService investigationService) {
        this.investigationService = investigationService;
    }

    @PostMapping("/ask")
    public AskResponse ask(@RequestBody AskRequest request) {
        return investigationService.investigate(request == null ? null : request.question());
    }

    /** Bad input (empty or too long question) -> 400 with a clear message. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    /** LLM provider down, rate-limited, tool-call limit hit, unparseable answer... -> 502, details only in the log. */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, String>> upstreamFailure(RuntimeException e) {
        log.error("Investigation failed", e);
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "The investigation could not be completed. Please try again."));
    }
}
