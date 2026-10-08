package com.college.portals;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * One code base, four services. The Spring PROFILE decides which one starts:
 *   hod (8082)   parent (8083)   advisor (8084)   notify (8085)
 * Every service has its own port, its own controllers and its own login role.
 */
@SpringBootApplication
public class PortalsApplication {
    public static void main(String[] args) {
        SpringApplication.run(PortalsApplication.class, args);
    }
}
