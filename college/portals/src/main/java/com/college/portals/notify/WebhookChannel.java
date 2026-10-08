package com.college.portals.notify;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/** Optional: POSTs the notification as JSON to a URL (a bridge to a real SMS / WhatsApp / Telegram gateway). */
public class WebhookChannel implements Channel {

    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final URI uri;

    public WebhookChannel(String url) {
        this.uri = URI.create(url);
    }

    @Override
    public String name() {
        return "webhook";
    }

    @Override
    public void send(Message m) throws IOException, InterruptedException {
        String json = "{\"id\":" + m.id() + ",\"to\":" + quote(m.destination()) + ",\"channel\":" + quote(m.channel())
                + ",\"recipientType\":" + quote(m.recipientType()) + ",\"title\":" + quote(m.title()) + ",\"message\":" + quote(m.body()) + "}";
        HttpResponse<String> res = client.send(HttpRequest.newBuilder(uri).timeout(Duration.ofSeconds(8))
                .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(json)).build(),
                HttpResponse.BodyHandlers.ofString());
        if (res.statusCode() >= 300) {
            throw new IOException("Webhook answered HTTP " + res.statusCode());
        }
    }

    private static String quote(String s) {
        if (s == null) {
            return "null";
        }
        StringBuilder sb = new StringBuilder("\"");
        for (char c : s.toCharArray()) {
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c < 0x20 ? " " : String.valueOf(c));
            }
        }
        return sb.append('"').toString();
    }
}
