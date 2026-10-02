package com.byteforce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;

/**
 * ByteForce web application entry point.
 * Bootstraps the Spring Boot Web and Thymeleaf presentation layer while
 * delegating persistence, business services, and security to the core ByteForce backend.
 */
@SpringBootApplication(exclude = {
        DataSourceAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class
})
public class ByteForceWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(ByteForceWebApplication.class, args);
    }
}
