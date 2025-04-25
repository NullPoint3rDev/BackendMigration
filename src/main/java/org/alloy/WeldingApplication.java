package org.alloy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan("org.alloy.models")
@EnableJpaRepositories("org.alloy.repositories")
public class WeldingApplication {
    public static void main(String[] args) {
        SpringApplication.run(WeldingApplication.class, args);
    }
}