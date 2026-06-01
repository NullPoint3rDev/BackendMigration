package org.alloy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

// @EntityScan/@EnableJpaRepositories убраны намеренно: entities (org.alloy.models.*) и
// repositories (org.alloy.repositories) — под-пакеты org.alloy, их находит авто-конфигурация Spring Boot.
// Явные аннотации заставляли @WebMvcTest поднимать все JpaRepository без EntityManagerFactory.
@SpringBootApplication
@EnableScheduling
@EnableAsync
public class WeldingApplication {
    public static void main(String[] args) {
        SpringApplication.run(WeldingApplication.class, args);
    }
}