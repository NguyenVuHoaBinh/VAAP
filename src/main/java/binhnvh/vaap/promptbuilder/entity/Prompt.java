package binhnvh.vaap.promptbuilder.entity;

import jakarta.persistence.*;
import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.Set;

import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "prompts")
public class Prompt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty
    @Size(max = 100)
    @Column(nullable = false, unique = true)
    private String name;

    @Size(max = 500)
    private String description;

    @NotEmpty
    @Column(columnDefinition = "TEXT", nullable = false)
    private String template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "llm_provider_id", nullable = false)
    private LLMProvider llmProvider;

    @Column(name = "model_name", nullable = false)
    @NotEmpty
    private String modelName;

    @Column(name = "is_active")
    private boolean active = true;

    @Column(name = "current_version")
    private Integer currentVersion = 1;

    @OneToMany(mappedBy = "prompt", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<PromptVersion> versions;

    @OneToMany(mappedBy = "prompt", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<PromptVariable> variables;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", nullable = false)
    @NotEmpty
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}