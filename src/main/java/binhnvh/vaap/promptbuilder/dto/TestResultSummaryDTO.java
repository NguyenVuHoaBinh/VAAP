package binhnvh.vaap.promptbuilder.dto;

public record TestResultSummaryDTO(
        Long promptId,
        Double avgExecutionTime,
        Double avgTokenCount,
        Double avgCost,
        Double avgSuccessRate,
        Double avgScore
) {}
