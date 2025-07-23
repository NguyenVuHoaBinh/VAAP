package binhnvh.vaap.promptbuilder.dto;

import java.time.LocalDateTime;

public record PromptVersionDTO(
        Long id,
        Integer versionNumber,
        String template,
        String changeDescription,
        LocalDateTime createdAt,
        String createdBy
) {}
