package com.college.portals.notify;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Switches the @Scheduled polling on - only in the notify service. */
@Configuration
@EnableScheduling
@Profile("notify")
public class NotifyConfig {
}
