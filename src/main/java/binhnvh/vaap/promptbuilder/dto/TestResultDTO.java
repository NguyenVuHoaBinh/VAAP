package binhnvh.vaap.promptbuilder.dto;

import binhnvh.vaap.promptbuilder.entity.PromptTest;

import java.time.LocalDateTime;
import java.util.List;

public record TestResultDTO(
        Long testId,
        String testName,
        PromptTest.TestStatus status,
        List<PromptResultDTO> results,
        LocalDateTime startedAt,
        LocalDateTime completedAt
) {

    public record PromptResultDTO(
            Long promptId,
            String promptName,
            Double averageExecutionTime,
            Double averageTokenCount,
            Double averageCost,
            Double successRate,
            Double averageScore
    ) {}
}
