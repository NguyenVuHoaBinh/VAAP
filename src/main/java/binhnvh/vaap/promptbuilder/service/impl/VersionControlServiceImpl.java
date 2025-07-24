package binhnvh.vaap.promptbuilder.service.impl;

import binhnvh.vaap.promptbuilder.dto.PromptVersionDTO;
import binhnvh.vaap.promptbuilder.entity.Prompt;
import binhnvh.vaap.promptbuilder.entity.PromptVersion;
import binhnvh.vaap.promptbuilder.exception.PromptBuilderException;
import binhnvh.vaap.promptbuilder.repository.PromptRepository;
import binhnvh.vaap.promptbuilder.repository.PromptVersionRepository;
import binhnvh.vaap.promptbuilder.service.ElasticsearchService;
import binhnvh.vaap.promptbuilder.service.VersionControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VersionControlServiceImpl implements VersionControlService {

    @Autowired
    private PromptVersionRepository versionRepository;

    @Autowired
    private PromptRepository promptRepository;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Override
    @Transactional
    public PromptVersionDTO createVersion(Long promptId, String template, String changeDescription, String userId) {
        Prompt prompt = promptRepository.findById(promptId)
                .orElseThrow(() -> new PromptBuilderException(
                        PromptBuilderException.ErrorCode.PROMPT_NOT_FOUND,
                        "Prompt not found with id: " + promptId
                ));

        // Get the next version number
        Integer maxVersion = versionRepository.findMaxVersionByPromptId(promptId)
                .orElse(0);
        Integer newVersionNumber = maxVersion + 1;

        // Create new version
        PromptVersion version = new PromptVersion();
        version.setPrompt(prompt);
        version.setVersionNumber(newVersionNumber);
        version.setTemplate(template);
        version.setChangeDescription(changeDescription);
        version.setCreatedBy(userId);

        PromptVersion savedVersion = versionRepository.save(version);

        // Update prompt's current version
        prompt.setCurrentVersion(newVersionNumber);
        promptRepository.save(prompt);

        return convertToDTO(savedVersion);
    }

    @Override
    public List<PromptVersionDTO> getVersionHistory(Long promptId) {
        // Verify prompt exists
        if (!promptRepository.existsById(promptId)) {
            throw new PromptBuilderException(
                    PromptBuilderException.ErrorCode.PROMPT_NOT_FOUND,
                    "Prompt not found with id: " + promptId
            );
        }

        List<PromptVersion> versions = versionRepository.findAllByPromptIdOrderByVersionDesc(promptId);

        return versions.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public PromptVersionDTO getVersion(Long promptId, Integer versionNumber) {
        PromptVersion version = versionRepository.findByPromptIdAndVersion(promptId, versionNumber)
                .orElseThrow(() -> new PromptBuilderException(
                        PromptBuilderException.ErrorCode.PROMPT_NOT_FOUND,
                        String.format("Version %d not found for prompt %d", versionNumber, promptId)
                ));

        return convertToDTO(version);
    }

    @Override
    @Transactional
    public PromptVersionDTO rollbackToVersion(Long promptId, Integer versionNumber, String userId) {
        // Get the prompt
        Prompt prompt = promptRepository.findByIdAndActiveTrue(promptId)
                .orElseThrow(() -> new PromptBuilderException(
                        PromptBuilderException.ErrorCode.PROMPT_NOT_FOUND,
                        "Prompt not found with id: " + promptId
                ));

        // Verify user has permission
        if (!prompt.getCreatedBy().equals(userId)) {
            throw new PromptBuilderException(
                    PromptBuilderException.ErrorCode.PERMISSION_DENIED,
                    "You don't have permission to rollback this prompt"
            );
        }

        // Get the target version
        PromptVersion targetVersion = versionRepository.findByPromptIdAndVersion(promptId, versionNumber)
                .orElseThrow(() -> new PromptBuilderException(
                        PromptBuilderException.ErrorCode.PROMPT_NOT_FOUND,
                        String.format("Version %d not found for prompt %d", versionNumber, promptId)
                ));

        // Create a new version with the rollback
        String changeDesc = String.format("Rolled back to version %d", versionNumber);
        PromptVersionDTO newVersion = createVersion(
                promptId,
                targetVersion.getTemplate(),
                changeDesc,
                userId
        );

        // Update the prompt's template
        prompt.setTemplate(targetVersion.getTemplate());
        promptRepository.save(prompt);

        // Update Elasticsearch
        try {
            elasticsearchService.updatePromptIndex(convertPromptToResponseDTO(prompt));
        } catch (Exception e) {
            // Log error but don't fail the rollback
            // In production, this should be handled properly
        }

        return newVersion;
    }

    @Override
    public Integer getLatestVersionNumber(Long promptId) {
        // Verify prompt exists
        if (!promptRepository.existsById(promptId)) {
            throw new PromptBuilderException(
                    PromptBuilderException.ErrorCode.PROMPT_NOT_FOUND,
                    "Prompt not found with id: " + promptId
            );
        }

        return versionRepository.findMaxVersionByPromptId(promptId)
                .orElse(1);
    }

    private PromptVersionDTO convertToDTO(PromptVersion version) {
        return new PromptVersionDTO(
                version.getId(),
                version.getVersionNumber(),
                version.getTemplate(),
                version.getChangeDescription(),
                version.getCreatedAt(),
                version.getCreatedBy()
        );
    }

    // Helper method - in production this would be in a shared utility class
    private binhnvh.vaap.promptbuilder.dto.PromptResponseDTO convertPromptToResponseDTO(Prompt prompt) {
        return new binhnvh.vaap.promptbuilder.dto.PromptResponseDTO(
                prompt.getId(),
                prompt.getName(),
                prompt.getDescription(),
                prompt.getTemplate(),
                prompt.getLlmProvider().getProviderName(),
                prompt.getModelName(),
                prompt.isActive(),
                prompt.getCurrentVersion(),
                null, // Variables would be loaded separately
                prompt.getCreatedAt(),
                prompt.getUpdatedAt(),
                prompt.getCreatedBy()
        );
    }
}
