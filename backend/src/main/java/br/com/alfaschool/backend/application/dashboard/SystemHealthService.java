package br.com.alfaschool.backend.application.dashboard;

import br.com.alfaschool.backend.application.dashboard.dto.SystemHealthDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.info.BuildProperties;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

@Service
public class SystemHealthService {

    private final HealthEndpoint healthEndpoint;
    private final String activeProfile;
    private final BuildProperties buildProperties;

    public SystemHealthService(HealthEndpoint healthEndpoint,
                               @Value("${spring.profiles.active:dev}") String activeProfile,
                               Optional<BuildProperties> buildProperties) {
        this.healthEndpoint = healthEndpoint;
        this.activeProfile = activeProfile;
        this.buildProperties = buildProperties.orElse(null);
    }

    public SystemHealthDTO currentHealth() {
        HealthComponent healthComponent = healthEndpoint.health();
        String status = healthComponent.getStatus().getCode();

        String db = "UNKNOWN";
        String diskSpace = "UNKNOWN";

        if (healthComponent instanceof Health health) {
            Map<String, Object> details = health.getDetails();
            db = statusFromDetails(details.get("db"));
            diskSpace = statusFromDetails(details.get("diskSpace"));
        }

        String version = buildProperties != null ? buildProperties.getVersion() : "0.0.1";
        return new SystemHealthDTO(status, db, diskSpace, activeProfile.toUpperCase(), version);
    }

    private String statusFromDetails(Object component) {
        if (component instanceof Map<?, ?> map) {
            Object status = map.get("status");
            return status == null ? "UNKNOWN" : String.valueOf(status).toUpperCase();
        }
        return "UNKNOWN";
    }
}
