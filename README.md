# AI-Powered Production Troubleshooting Assistant

A Java 21 / Spring Boot 4 / Spring AI 2 application that exposes troubleshooting tools
(log search, service health, similar-incident lookup) over the **Model Context Protocol (MCP)**
so AI clients like Claude Desktop can investigate production issues in natural language.

> Work in progress. Full write-up, architecture diagram and demo coming soon.

## Run locally

```bash
docker compose up -d      # Postgres 16 + pgvector
mvn spring-boot:run       # or run TroubleshootingAssistantApplication from IntelliJ
curl http://localhost:8080/actuator/health
```

## Mock data
`src/main/resources/data/` contains sample logs for three services, current service health,
and 20 historical incidents used for similarity search.
