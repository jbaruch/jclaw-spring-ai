package com.codepocalypse.agent;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
public class AgentTools {

    private final HttpClient http = HttpClient.newHttpClient();

    @Tool(description = "Get the current date and time")
    public String getCurrentTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    @Tool(description = "Get the current weather for a city (simulated)")
    public String getWeather(@ToolParam(description = "City name") String city) {
        return "Weather in " + city + ": 22°C, sunny with light clouds";
    }

    @Tool(description = "Introspect yourself. Read your own performance metrics from the actuator. Returns token usage, call counts, response times, and health status. Use this when asked about your own performance, health, cost, or how many tokens you have consumed.")
    public String introspect() {
        try {
            var metrics = fetch("http://localhost:8080/actuator/metrics");
            var health = fetch("http://localhost:8080/actuator/health");
            var aiMetrics = new StringBuilder("=== JClaw Self-Diagnostics ===\n\n");
            aiMetrics.append("Health: ").append(health).append("\n\n");

            for (String metric : new String[]{
                    "gen_ai.client.operation",
                    "gen_ai.client.token.usage",
                    "spring.ai.chat.client",
                    "spring.ai.tool"}) {
                try {
                    var detail = fetch("http://localhost:8080/actuator/metrics/" + metric);
                    aiMetrics.append("Metric [").append(metric).append("]: ").append(detail).append("\n\n");
                } catch (Exception e) {
                    aiMetrics.append("Metric [").append(metric).append("]: not available yet\n\n");
                }
            }
            return aiMetrics.toString();
        } catch (Exception e) {
            return "Self-diagnostics failed: " + e.getMessage();
        }
    }

    private String fetch(String url) throws Exception {
        var req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
        return http.send(req, HttpResponse.BodyHandlers.ofString()).body();
    }
}
