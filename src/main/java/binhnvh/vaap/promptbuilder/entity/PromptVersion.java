package binhnvh.vaap.promptbuilder.entity;

import jakarta.persistence.*;
import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import java.time.LocalDateTime;

@Entity
@Data
@Table(name = "prompt_versions")
public class PromptVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_id", nullable = false)
    private Prompt prompt;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @NotEmpty
    @Column(columnDefinition = "TEXT", nullable = false)
    private String template;

    @Column(name = "change_description")
    private String changeDescription;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false)
    @NotEmpty
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}