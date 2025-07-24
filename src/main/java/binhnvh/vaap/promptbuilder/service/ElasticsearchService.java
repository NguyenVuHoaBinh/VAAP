package binhnvh.vaap.promptbuilder.service;

import binhnvh.vaap.promptbuilder.dto.PromptResponseDTO;

import java.util.List;
import java.util.Map;

public interface ElasticsearchService {
    void indexPrompt(PromptResponseDTO prompt);
    void updatePromptIndex(PromptResponseDTO prompt);
    void deletePromptFromIndex(Long promptId);
    List<PromptResponseDTO> searchPrompts(String query, Map<String, Object> filters);
    List<PromptResponseDTO> searchByTemplate(String templateQuery);
    List<PromptResponseDTO> searchByVariables(List<String> variableNames);
}