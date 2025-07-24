package binhnvh.vaap.promptbuilder.service.impl;

import binhnvh.vaap.promptbuilder.dto.*;
import binhnvh.vaap.promptbuilder.entity.LLMProvider;
import binhnvh.vaap.promptbuilder.entity.Prompt;
import binhnvh.vaap.promptbuilder.entity.PromptVariable;
import binhnvh.vaap.promptbuilder.repository.LLMProviderRepository;
import binhnvh.vaap.promptbuilder.repository.PromptRepository;
import binhnvh.vaap.promptbuilder.repository.PromptVariableRepository;
import binhnvh.vaap.promptbuilder.service.ElasticsearchService;
import binhnvh.vaap.promptbuilder.service.LLMService;
import binhnvh.vaap.promptbuilder.service.PromptService;
import binhnvh.vaap.promptbuilder.service.VersionControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class PromptServiceImpl implements PromptService {

    @Autowired
    private PromptRepository promptRepository;

    @Autowired
    private PromptVariableRepository variableRepository;

    @Autowired
    private LLMProviderRepository providerRepository;

    @Autowired
    private VersionControlService versionControlService;

    @Autowired
    private ElasticsearchService elasticsearchService;

    @Autowired
    private LLMService llmService;

    @Override
    @Transactional
    public PromptResponseDTO createPrompt(CreatePromptDTO createDTO, String userId) {
        // Check if prompt name already exists
        if (promptRepository.existsByNameAndActiveTrue(createDTO.name())) {
            throw new IllegalArgumentException("Prompt with name '" + createDTO.name() + "' already exists");
        }

        // Validate LLM provider
        LLMProvider provider = providerRepository.findById(createDTO.llmProviderId())
                .orElseThrow(() -> new IllegalArgumentException("LLM Provider not found"));

        // Create prompt entity
        Prompt prompt = new Prompt();
        prompt.setName(createDTO.name());
        prompt.setDescription(createDTO.description());
        prompt.setTemplate(createDTO.template());
        prompt.setLlmProvider(provider);
        prompt.setModelName(createDTO.modelName());
        prompt.setCreatedBy(userId);
        prompt.setActive(true);

        Prompt savedPrompt = promptRepository.save(prompt);

        // Save variables if any
        if (createDTO.variables() != null && !createDTO.variables().isEmpty()) {
            saveVariables(savedPrompt, createDTO.variables());
        }

        // Create initial version
        versionControlService.createVersion(savedPrompt.getId(), createDTO.template(), "Initial version", userId);

        // Index in Elasticsearch
        PromptResponseDTO responseDTO = convertToResponseDTO(savedPrompt);
        elasticsearchService.indexPrompt(responseDTO);

        return responseDTO;
    }

    @Override
    @Transactional
    public PromptResponseDTO updatePrompt(Long promptId, UpdatePromptDTO updateDTO, String userId) {
        Prompt prompt = promptRepository.findByIdAndActiveTrue(promptId)
                .orElseThrow(() -> new IllegalArgumentException("Prompt not found"));

        boolean hasChanges = false;
        StringBuilder changeDescription = new StringBuilder();

        // Update name if provided
        if (updateDTO.name() != null && !updateDTO.name().equals(prompt.getName())) {
            if (promptRepository.existsByNameAndActiveTrue(updateDTO.name())) {
                throw new IllegalArgumentException("Prompt name already exists");
            }
            prompt.setName(updateDTO.name());
            changeDescription.append("Updated name. ");
            hasChanges = true;
        }

        // Update description if provided
        if (updateDTO.description() != null) {
            prompt.setDescription(updateDTO.description());
            hasChanges = true;
        }

        // Update template if provided
        if (updateDTO.template() != null && !updateDTO.template().equals(prompt.getTemplate())) {
            prompt.setTemplate(updateDTO.template());
            changeDescription.append("Updated template. ");
            hasChanges = true;

            // Create new version
            versionControlService.createVersion(
                    promptId,
                    updateDTO.template(),
                    updateDTO.changeDescription() != null ? updateDTO.changeDescription() : changeDescription.toString(),
                    userId
            );
        }

        // Update provider if provided
        if (updateDTO.llmProviderId() != null) {
            LLMProvider provider = providerRepository.findById(updateDTO.llmProviderId())
                    .orElseThrow(() -> new IllegalArgumentException("LLM Provider not found"));
            prompt.setLlmProvider(provider);
            changeDescription.append("Changed LLM provider. ");
            hasChanges = true;
        }

        // Update model if provided
        if (updateDTO.modelName() != null) {
            prompt.setModelName(updateDTO.modelName());
            changeDescription.append("Changed model. ");
            hasChanges = true;
        }

        // Update variables if provided
        if (updateDTO.variables() != null) {
            variableRepository.deleteAllByPromptId(promptId);
            saveVariables(prompt, updateDTO.variables());
            changeDescription.append("Updated variables. ");
            hasChanges = true;
        }

        if (!hasChanges) {
            throw new IllegalArgumentException("No changes detected");
        }

        Prompt updatedPrompt = promptRepository.save(prompt);
        PromptResponseDTO responseDTO = convertToResponseDTO(updatedPrompt);

        // Update Elasticsearch index
        elasticsearchService.updatePromptIndex(responseDTO);

        return responseDTO;
    }

    @Override
    public PromptResponseDTO getPromptById(Long promptId) {
        Prompt prompt = promptRepository.findByIdAndActiveTrue(promptId)
                .orElseThrow(() -> new IllegalArgumentException("Prompt not found"));
        return convertToResponseDTO(prompt);
    }

    @Override
    public List<PromptResponseDTO> getAllPromptsByUser(String userId) {
        List<Prompt> prompts = promptRepository.findAllActiveByUser(userId);
        return prompts.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<PromptResponseDTO> getAllPromptsByProvider(Long providerId) {
        List<Prompt> prompts = promptRepository.findAllByProvider(providerId);
        return prompts.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deletePrompt(Long promptId, String userId) {
        Prompt prompt = promptRepository.findByIdAndActiveTrue(promptId)
                .orElseThrow(() -> new IllegalArgumentException("Prompt not found"));

        if (!prompt.getCreatedBy().equals(userId)) {
            throw new IllegalArgumentException("You don't have permission to delete this prompt");
        }

        prompt.setActive(false);
        promptRepository.save(prompt);

        // Remove from Elasticsearch
        elasticsearchService.deletePromptFromIndex(promptId);
    }

    @Override
    @Transactional
    public PromptResponseDTO duplicatePrompt(Long promptId, String newName, String userId) {
        Prompt originalPrompt = promptRepository.findByIdAndActiveTrue(promptId)
                .orElseThrow(() -> new IllegalArgumentException("Prompt not found"));

        // Create copy
        Prompt duplicate = new Prompt();
        duplicate.setName(newName);
        duplicate.setDescription(originalPrompt.getDescription() + " (Copy)");
        duplicate.setTemplate(originalPrompt.getTemplate());
        duplicate.setLlmProvider(originalPrompt.getLlmProvider());
        duplicate.setModelName(originalPrompt.getModelName());
        duplicate.setCreatedBy(userId);
        duplicate.setActive(true);

        Prompt savedDuplicate = promptRepository.save(duplicate);

        // Copy variables
        List<PromptVariable> originalVariables = variableRepository.findAllByPromptId(promptId);
        for (PromptVariable originalVar : originalVariables) {
            PromptVariable duplicateVar = new PromptVariable();
            duplicateVar.setPrompt(savedDuplicate);
            duplicateVar.setVariableName(originalVar.getVariableName());
            duplicateVar.setDefaultValue(originalVar.getDefaultValue());
            duplicateVar.setDescription(originalVar.getDescription());
            duplicateVar.setRequired(originalVar.isRequired());
            duplicateVar.setDataType(originalVar.getDataType());
            variableRepository.save(duplicateVar);
        }

        // Create initial version
        versionControlService.createVersion(savedDuplicate.getId(), savedDuplicate.getTemplate(),
                "Duplicated from prompt: " + originalPrompt.getName(), userId);

        PromptResponseDTO responseDTO = convertToResponseDTO(savedDuplicate);
        elasticsearchService.indexPrompt(responseDTO);

        return responseDTO;
    }

    @Override
    //TODO: modify parameter execs
    public String executePrompt(ExecutePromptDTO executeDTO) {
        Prompt prompt = promptRepository.findByIdAndActiveTrue(executeDTO.promptId())
                .orElseThrow(() -> new IllegalArgumentException("Prompt not found"));

        // Replace variables in template
        String processedPrompt = processTemplate(prompt.getTemplate(), executeDTO.variables());

        // Validate all required variables are provided
        List<PromptVariable> variables = variableRepository.findAllByPromptId(executeDTO.promptId());
        for (PromptVariable var : variables) {
            if (var.isRequired() && !executeDTO.variables().containsKey(var.getVariableName())) {
                throw new IllegalArgumentException("Required variable '" + var.getVariableName() + "' not provided");
            }
        }

        // Call LLM
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("temperature", 0.7);
        parameters.put("max_tokens", 1000);

        return llmService.callLLM(prompt.getLlmProvider().getId(), prompt.getModelName(),
                processedPrompt, parameters);
    }

    @Override
    public void indexPromptInElasticsearch(Long promptId) {
        PromptResponseDTO prompt = getPromptById(promptId);
        elasticsearchService.indexPrompt(prompt);
    }

    private void saveVariables(Prompt prompt, List<VariableDTO> variables) {
        for (VariableDTO varDTO : variables) {
            PromptVariable variable = new PromptVariable();
            variable.setPrompt(prompt);
            variable.setVariableName(varDTO.variableName());
            variable.setDefaultValue(varDTO.defaultValue());
            variable.setDescription(varDTO.description());
            variable.setRequired(varDTO.required());
            variable.setDataType(varDTO.dataType());
            variableRepository.save(variable);
        }
    }

    private String processTemplate(String template, Map<String, Object> variables) {
        if (variables == null || variables.isEmpty()) {
            return template;
        }

        String processed = template;
        Pattern pattern = Pattern.compile("\\{\\{(\\w+)\\}\\}");
        Matcher matcher = pattern.matcher(template);

        while (matcher.find()) {
            String varName = matcher.group(1);
            Object value = variables.get(varName);
            if (value != null) {
                processed = processed.replace("{{" + varName + "}}", value.toString());
            }
        }

        return processed;
    }

    private PromptResponseDTO convertToResponseDTO(Prompt prompt) {
        List<VariableDTO> variableDTOs = prompt.getVariables() != null
                ? prompt.getVariables().stream()
                .map(var -> new VariableDTO(
                        var.getVariableName(),
                        var.getDefaultValue(),
                        var.getDescription(),
                        var.isRequired(),
                        var.getDataType()
                ))
                .collect(Collectors.toList())
                : new ArrayList<>();

        return new PromptResponseDTO(
                prompt.getId(),
                prompt.getName(),
                prompt.getDescription(),
                prompt.getTemplate(),
                prompt.getLlmProvider().getProviderName(),
                prompt.getModelName(),
                prompt.isActive(),
                prompt.getCurrentVersion(),
                variableDTOs,
                prompt.getCreatedAt(),
                prompt.getUpdatedAt(),
                prompt.getCreatedBy()
        );
    }
}