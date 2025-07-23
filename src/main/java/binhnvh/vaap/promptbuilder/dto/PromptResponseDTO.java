package binhnvh.vaap.promptbuilder.dto;

import java.time.LocalDateTime;
import java.util.List;

public record PromptResponseDTO(
        Long id,
        String name,
        String description,
        String template,
        String llmProviderName,
        String modelName,
        boolean active,
        Integer currentVersion,
        List<VariableDTO> variables,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy
) {}
