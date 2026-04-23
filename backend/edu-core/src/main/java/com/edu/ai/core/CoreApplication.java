package com.edu.ai.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * Entry point for the edu-core service.
 *
 * <p>Scans the {@code com.edu.ai.common} package so shared beans
 * ({@code GlobalExceptionHandler}, future {@code R}-aware utilities) are
 * activated when running as a Spring MVC service. {@code edu-gateway}
 * intentionally does NOT scan this package because it runs on WebFlux.
 */
@SpringBootApplication
@ComponentScan(basePackages = {"com.edu.ai.core", "com.edu.ai.common"})
public class CoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreApplication.class, args);
    }
}
