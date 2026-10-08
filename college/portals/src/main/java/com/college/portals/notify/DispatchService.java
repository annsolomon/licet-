package com.college.portals.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * NOTIFICATION SERVICE (port 8085).
 *   Oracle trigger -> NOTIFICATION row (PENDING) -> this service polls every few seconds
 *   -> LogChannel (always) [+ WebhookChannel when notify.webhook-url is set] -> row becomes SENT (or FAILED)
 * Because the trigger lives in the database, an absence is notified whichever application marked it.
 */
@Service
@Profile("notify")
public class DispatchService {

    private static final Logger LOG = LoggerFactory.getLogger(DispatchService.class);

    private final NotificationRepository repository;
    private final List<Channel> channels = new ArrayList<>();

    public DispatchService(NotificationRepository repository,
                           @Value("${notify.log-file}") String logFile,
                           @Value("${notify.webhook-url:}") String webhookUrl) {
        this.repository = repository;
        channels.add(new LogChannel(Path.of(logFile)));
        if (webhookUrl != null && !webhookUrl.isBlank()) {
            channels.add(new WebhookChannel(webhookUrl.trim()));
        }
        LOG.info("Notification channels: {}", channels.stream().map(Channel::name).toList());
    }

    /** Runs by itself (see @EnableScheduling in NotifyConfig) and when POST /api/dispatch is called. */
    @Scheduled(fixedDelayString = "${notify.poll-ms:3000}")
    public Map<String, Integer> dispatch() {
        int sent = 0;
        int failed = 0;
        for (Channel.Message m : repository.pending(200)) {
            try {
                for (Channel c : channels) {
                    c.send(m);
                }
                repository.markSent(m.id());
                sent++;
            } catch (Exception e) {
                repository.markFailed(m.id(), e.getMessage());
                failed++;
                LOG.warn("Notification {} failed: {}", m.id(), e.getMessage());
            }
        }
        if (sent + failed > 0) {
            LOG.info("Dispatched {} notification(s), {} failed", sent, failed);
        }
        return Map.of("sent", sent, "failed", failed);
    }

    public int retryFailed() {
        return repository.retryFailed();
    }
}
