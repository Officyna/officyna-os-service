package br.com.officyna.seed;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "database.seed",
        name = "enabled",
        havingValue = "true"
)
public class DatabaseSeeder implements CommandLineRunner {

    private final CustomerSeeder customerSeeder;
    private final VehicleSeeder vehicleSeeder;

    @Override
    public void run(String... args) {

        customerSeeder.seed();

        vehicleSeeder.seed();

        log.info("==========================================");
        log.info("DATABASE SEED FINALIZADO COM SUCESSO");
        log.info("==========================================");
    }
}