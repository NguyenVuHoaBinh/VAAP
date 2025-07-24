package binhnvh.vaap.promptbuilder.service;

import binhnvh.vaap.promptbuilder.dto.PromptVersionDTO;

import java.util.List;

public interface VersionControlService {
    PromptVersionDTO createVersion(Long promptId, String template, String changeDescription, String userId);
    List<PromptVersionDTO> getVersionHistory(Long promptId);
    PromptVersionDTO getVersion(Long promptId, Integer versionNumber);
    PromptVersionDTO rollbackToVersion(Long promptId, Integer versionNumber, String userId);
    Integer getLatestVersionNumber(Long promptId);
}
