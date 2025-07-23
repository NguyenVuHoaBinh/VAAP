package binhnvh.vaap.promptbuilder.entity;

import jakarta.persistence.*;
import lombok.Data;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Data
@Table(name = "prompt_tests")
public class PromptTest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotEmpty
    @Column(name = "test_name", nullable = false)
    private String testName;

    @Enumerated(EnumType.STRING)
    @Column(name = "test_type", nullable = false)
    private TestType testType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_a_id", nullable = false)
    private Prompt promptA;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_b_id")
    private Prompt promptB; // For A/B testing

    @Min(1)
    @Column(name = "sample_size")
    private Integer sampleSize = 100;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TestStatus status = TestStatus.CREATED;

    @OneToMany(mappedBy = "promptTest", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    private Set<PromptTestResult> results;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "created_by", nullable = false)
    @NotEmpty
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum TestType {
        SINGLE_TEST, AB_TEST, AUTO_OPTIMIZATION
    }

    public enum TestStatus {
        CREATED, RUNNING, COMPLETED, FAILED, CANCELLED
    }
}
