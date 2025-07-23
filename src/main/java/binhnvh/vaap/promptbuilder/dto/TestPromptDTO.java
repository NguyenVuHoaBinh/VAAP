package binhnvh.vaap.promptbuilder.dto;

import binhnvh.vaap.promptbuilder.entity.PromptTest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record TestPromptDTO(
        @NotEmpty String testName,
        @NotNull PromptTest.TestType testType,
        @NotNull Long promptAId,
        Long promptBId,
        @Min(1) Integer sampleSize,
        Map<String, Object> testVariables
) {
    public TestPromptDTO {
        if (testName == null || testName.isBlank()) {
            throw new IllegalArgumentException("Test name cannot be empty");
        }
        if (testType == null) {
            throw new IllegalArgumentException("Test type must be specified");
        }
        if (promptAId == null || promptAId <= 0) {
            throw new IllegalArgumentException("Invalid prompt A ID");
        }
        if (testType == PromptTest.TestType.AB_TEST && (promptBId == null || promptBId <= 0)) {
            throw new IllegalArgumentException("Prompt B is required for A/B testing");
        }
        if (sampleSize == null || sampleSize < 1) {
            sampleSize = 100;
        }
    }
}