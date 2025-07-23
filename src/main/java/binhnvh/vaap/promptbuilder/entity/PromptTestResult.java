package binhnvh.vaap.promptbuilder.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Data
@Table(name = "prompt_test_results")
public class PromptTestResult {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_test_id", nullable = false)
    private PromptTest promptTest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_id", nullable = false)
    private Prompt prompt;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "cost")
    private Double cost;

    @Column(name = "success_rate")
    private Double successRate;

    @Column(name = "average_score")
    private Double averageScore;

    @ElementCollection
    @CollectionTable(name = "test_metrics", joinColumns = @JoinColumn(name = "result_id"))
    @MapKeyColumn(name = "metric_name")
    @Column(name = "metric_value")
    private Map<String, String> metrics;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
