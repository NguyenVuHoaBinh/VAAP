package binhnvh.vaap.promptbuilder.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
public class HealthCheckController {

    @Autowired
    private VaultService vaultService;

    @GetMapping
    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", System.currentTimeMillis());

        // Check Vault connectivity
        boolean vaultHealthy = vaultService.validateVaultConnection();
        health.put("vault", vaultHealthy ? "UP" : "DOWN");

        return health;
    }
}
