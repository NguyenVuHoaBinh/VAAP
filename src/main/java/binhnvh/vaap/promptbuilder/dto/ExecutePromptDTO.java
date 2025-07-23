package binhnvh.vaap.promptbuilder.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Map;

public record ExecutePromptDTO(
        @NotNull Long promptId,
        Map<String, Object> variables,
        boolean saveResult
) {
    public ExecutePromptDTO {
        if (promptId == null || promptId <= 0) {
            throw new IllegalArgumentException("Invalid prompt ID");
        }
    }
}
