package com.codepocalypse.agent;

import com.williamcallahan.tui4j.compat.bubbletea.Command;
import com.williamcallahan.tui4j.compat.bubbletea.KeyPressMessage;
import com.williamcallahan.tui4j.compat.bubbletea.Message;
import com.williamcallahan.tui4j.compat.bubbletea.Model;
import com.williamcallahan.tui4j.compat.bubbletea.Program;
import com.williamcallahan.tui4j.compat.bubbletea.QuitMessage;
import com.williamcallahan.tui4j.compat.bubbletea.UpdateResult;
import com.williamcallahan.tui4j.compat.bubbles.cursor.Cursor;
import com.williamcallahan.tui4j.compat.bubbles.textarea.Textarea;
import com.williamcallahan.tui4j.compat.bubbles.viewport.Viewport;
import com.williamcallahan.tui4j.compat.lipgloss.Style;
import com.williamcallahan.tui4j.compat.lipgloss.color.Color;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;

/**
 * JClaw - Terminal chat client for the Codepocalypse Now personal AI agent.
 * Built with TUI4J (Java port of Bubble Tea) using the Elm Architecture.
 */
public class JClawTui implements Model {

    private final Textarea textarea;
    private final Viewport viewport;
    private final List<String> messages = new ArrayList<>();
    private final HttpClient http = HttpClient.newHttpClient();
    private final String baseUrl;
    private final String sessionId;
    private boolean waiting = false;

    private final Style userStyle = Style.newStyle().foreground(Color.color("12"));
    private final Style agentStyle = Style.newStyle().foreground(Color.color("10"));
    private final Style waitStyle = Style.newStyle().foreground(Color.color("8"));
    private final Style titleStyle = Style.newStyle().foreground(Color.color("5")).bold(true);

    public JClawTui(String baseUrl, String sessionId) {
        this.baseUrl = baseUrl;
        this.sessionId = sessionId;

        this.textarea = new Textarea();
        textarea.setPlaceholder("Ask your agent...");
        textarea.focus();
        textarea.setPrompt("┃ ");
        textarea.setCharLimit(500);
        textarea.setWidth(70);
        textarea.setHeight(3);
        textarea.setShowLineNumbers(false);

        this.viewport = Viewport.create(70, 18);
        // Don't call Style.render() here -- TerminalInfo isn't initialized until Program.run()
        viewport.setContent("JClaw - Personal AI Agent\nType a message and press Enter. Ctrl+C to quit.\n");
    }

    @Override
    public Command init() {
        // Now safe to use styles -- Program has initialized the terminal
        refreshViewport();
        return Cursor::blink;
    }

    @Override
    public UpdateResult<? extends Model> update(Message msg) {
        textarea.update(msg);
        viewport.update(msg);

        if (msg instanceof KeyPressMessage kpm) {
            if ("ctrl+c".equals(kpm.key()) || "esc".equals(kpm.key())) {
                return UpdateResult.from(this, QuitMessage::new);
            }
            if ("enter".equals(kpm.key()) && !waiting) {
                String input = textarea.value().trim();
                if (!input.isEmpty()) {
                    textarea.reset();
                    messages.add(userStyle.render("You: ") + input);
                    refreshViewport();
                    waiting = true;
                    return UpdateResult.from(this, () -> callAgent(input));
                }
            }
        }

        if (msg instanceof AgentResponse resp) {
            messages.add(agentStyle.render("Agent: ") + resp.content());
            refreshViewport();
            waiting = false;
        }

        return UpdateResult.from(this, null);
    }

    @Override
    public String view() {
        String status = waiting ? waitStyle.render("  ⏳ Thinking...") : "";
        return viewport.view() + "\n" + status + "\n" + textarea.view();
    }

    private void refreshViewport() {
        String header = titleStyle.render("🦞 JClaw") + " - Personal AI Agent\n\n";
        String content = header + String.join("\n", messages);
        int w = viewport.getWidth();
        if (w > 0) {
            content = Style.newStyle().width(w).render(content);
        }
        viewport.setContent(content);
        viewport.gotoBottom();
    }

    private Message callAgent(String input) {
        try {
            var req = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/chat?sessionId=" + sessionId))
                    .header("Content-Type", "text/plain")
                    .POST(HttpRequest.BodyPublishers.ofString(input))
                    .build();
            var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            return new AgentResponse(resp.body());
        } catch (Exception e) {
            return new AgentResponse("Error - " + e.getMessage());
        }
    }

    public record AgentResponse(String content) implements Message {}

    public static void main(String[] args) {
        String url = args.length > 0 ? args[0] : "http://localhost:8080";
        String session = args.length > 1 ? args[1] : "demo";
        new Program(new JClawTui(url, session)).withAltScreen().run();
    }
}
