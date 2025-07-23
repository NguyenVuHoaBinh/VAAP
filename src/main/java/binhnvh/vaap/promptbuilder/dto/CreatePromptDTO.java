package binhnvh.vaap.promptbuilder.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CreatePromptDTO(
        @NotEmpty String name,
        String description,
        @NotEmpty String template,
        @NotNull Long llmProviderId,
        @NotEmpty String modelName,
        List<VariableDTO> variables
) {
    public CreatePromptDTO {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Prompt name cannot be empty");
        }
        if (template == null || template.isBlank()) {
            throw new IllegalArgumentException("Prompt template cannot be empty");
        }
        if (llmProviderId == null || llmProviderId <= 0) {
            throw new IllegalArgumentException("Invalid LLM provider ID");
        }
        if (modelName == null || modelName.isBlank()) {
            throw new IllegalArgumentException("Model name cannot be empty");
        }
    }
}
