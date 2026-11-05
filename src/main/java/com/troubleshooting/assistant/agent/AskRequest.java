package com.troubleshooting.assistant.agent;

/** Body of POST /ask, e.g. {"question": "Why is payment-service failing?"} */
public record AskRequest(String question) {
}
