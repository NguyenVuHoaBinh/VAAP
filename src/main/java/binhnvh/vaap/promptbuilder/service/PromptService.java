package binhnvh.vaap.promptbuilder.service;

import binhnvh.vaap.promptbuilder.dto.CreatePromptDTO;
import binhnvh.vaap.promptbuilder.dto.ExecutePromptDTO;
import binhnvh.vaap.promptbuilder.dto.PromptResponseDTO;
import binhnvh.vaap.promptbuilder.dto.UpdatePromptDTO;

import java.util.List;

public interface PromptService {
    PromptResponseDTO createPrompt(CreatePromptDTO createPromptDTO, String userId);
    PromptResponseDTO updatePrompt(Long promptId, UpdatePromptDTO updatePromptDTO, String userId);
    PromptResponseDTO getPromptById(Long promptId);
    List<PromptResponseDTO> getAllPromptsByUser(String userId);
    List<PromptResponseDTO> getAllPromptsByProvider(Long providerId);
    void deletePrompt(Long promptId, String userId);
    PromptResponseDTO duplicatePrompt(Long promptId, String newName, String userId);
    String executePrompt(ExecutePromptDTO executePromptDTO);
    void indexPromptInElasticsearch(Long promptId);
}