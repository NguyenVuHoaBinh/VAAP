package binhnvh.vaap.promptbuilder.dto;

import binhnvh.vaap.promptbuilder.entity.PromptVariable;
import jakarta.validation.constraints.Pattern;

public record VariableDTO(
        @Pattern(regexp = "^[a-zA-Z_][a-zA-Z0-9_]*$") String variableName,
        String defaultValue,
        String description,
        boolean required,
        PromptVariable.VariableType dataType
) {
    public VariableDTO {
        if (variableName == null || variableName.isBlank()) {
            throw new IllegalArgumentException("Variable name cannot be empty");
        }
        if (dataType == null) {
            dataType = PromptVariable.VariableType.STRING;
        }
    }
}