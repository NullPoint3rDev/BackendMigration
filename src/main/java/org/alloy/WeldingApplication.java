package org.alloy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EntityScan("org.alloy.models")
@EnableJpaRepositories("org.alloy.repositories")
@EnableScheduling
@EnableCaching
public class WeldingApplication {
    public static void main(String[] args) {
        SpringApplication.run(WeldingApplication.class, args);
    }
}