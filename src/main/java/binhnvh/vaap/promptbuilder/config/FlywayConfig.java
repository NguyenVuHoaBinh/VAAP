package binhnvh.vaap.promptbuilder.config;

import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
public class FlywayConfig {

    @Value("${spring.flyway.clean-disabled:true}")
    private boolean cleanDisabled;

    /**
     * Custom Flyway migration strategy for development environment
     * Allows clean + migrate in development only
     */
    @Bean
    @Profile("dev")
    public FlywayMigrationStrategy cleanMigrateStrategy() {
        return flyway -> {
            if (!cleanDisabled) {
                flyway.clean();
            }
            flyway.repair();
            flyway.migrate();
        };
    }

    /**
     * Custom Flyway migration strategy for production environment
     * Only repairs and migrates, never cleans
     */
    @Bean
    @Profile("!dev")
    public FlywayMigrationStrategy safeMigrateStrategy() {
        return flyway -> {
            // Repair the Flyway metadata table
            flyway.repair();

            // Validate the applied migrations against the available ones
            flyway.validate();

            // Migrate
            flyway.migrate();
        };
    }
}
