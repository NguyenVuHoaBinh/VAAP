package binhnvh.vaap.promptbuilder.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "llm_providers")
public class LLMProvider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty
    @Pattern(regexp = "^[A-Z_]+$")
    @Column(name = "provider_name", nullable = false, unique = true)
    private String providerName;

    @NotEmpty
    @Column(name = "api_endpoint", nullable = false)
    private String apiEndpoint;

    @Column(name = "vault_path", nullable = false)
    @NotEmpty
    private String vaultPath; // Path in HashiCorp Vault for API key

    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "rate_limit")
    private Integer rateLimit;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}