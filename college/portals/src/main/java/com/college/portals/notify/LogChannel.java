package com.college.portals.notify;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;

/** Always on: prints "SMS to 8000000101: ..." in the console and appends it to output/notifications.log. */
public class LogChannel implements Channel {

    private static final Logger LOG = LoggerFactory.getLogger(LogChannel.class);
    private final Path file;

    public LogChannel(Path file) {
        this.file = file;
    }

    @Override
    public String name() {
        return "log";
    }

    @Override
    public synchronized void send(Message m) throws IOException {
        String line = LocalDateTime.now().withNano(0) + "  [" + m.channel() + " to " + m.recipientType() + " " + m.destination() + "]  "
                + m.title() + " | " + m.body();
        LOG.info(line);
        if (file.getParent() != null) {
            Files.createDirectories(file.getParent());
        }
        Files.writeString(file, line + System.lineSeparator(), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
}
