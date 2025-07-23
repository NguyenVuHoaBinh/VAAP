package binhnvh.vaap.promptbuilder.dto;

import java.util.List;

public record UpdatePromptDTO(
        String name,
        String description,
        String template,
        Long llmProviderId,
        String modelName,
        List<VariableDTO> variables,
        String changeDescription
) {
    public UpdatePromptDTO {
        if (name != null && name.isBlank()) {
            throw new IllegalArgumentException("Prompt name cannot be blank");
        }
        if (template != null && template.isBlank()) {
            throw new IllegalArgumentException("Prompt template cannot be blank");
        }
        if (llmProviderId != null && llmProviderId <= 0) {
            throw new IllegalArgumentException("Invalid LLM provider ID");
        }
        if (modelName != null && modelName.isBlank()) {
            throw new IllegalArgumentException("Model name cannot be blank");
        }
    }
}
