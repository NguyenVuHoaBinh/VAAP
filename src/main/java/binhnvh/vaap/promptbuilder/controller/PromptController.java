package binhnvh.vaap.promptbuilder.controller;

import binhnvh.vaap.promptbuilder.ApiResponse;
import binhnvh.vaap.promptbuilder.dto.CreatePromptDTO;
import binhnvh.vaap.promptbuilder.dto.ExecutePromptDTO;
import binhnvh.vaap.promptbuilder.dto.PromptResponseDTO;
import binhnvh.vaap.promptbuilder.dto.UpdatePromptDTO;
import binhnvh.vaap.promptbuilder.service.PromptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/prompts")
public class PromptController {

    @Autowired
    private PromptService promptService;

    @PostMapping
    public ResponseEntity<ApiResponse<PromptResponseDTO>> createPrompt(
            @Valid @RequestBody CreatePromptDTO createDTO,
            @RequestHeader("X-User-Id") String userId) {
        try {
            PromptResponseDTO prompt = promptService.createPrompt(createDTO, userId);
            ApiResponse<PromptResponseDTO> response = new ApiResponse<>(
                    "SUCCESS",
                    "Prompt created successfully",
                    prompt
            );
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            throw e;
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PromptResponseDTO>> updatePrompt(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePromptDTO updateDTO,
            @RequestHeader("X-User-Id") String userId) {
        try {
            PromptResponseDTO prompt = promptService.updatePrompt(id, updateDTO, userId);
            ApiResponse<PromptResponseDTO> response = new ApiResponse<>(
                    "SUCCESS",
                    "Prompt updated successfully",
                    prompt
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PromptResponseDTO>> getPrompt(@PathVariable Long id) {
        try {
            PromptResponseDTO prompt = promptService.getPromptById(id);
            ApiResponse<PromptResponseDTO> response = new ApiResponse<>(
                    "SUCCESS",
                    "Prompt retrieved successfully",
                    prompt
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw e;
        }
    }

    @GetMapping("/user")
    public ResponseEntity<ApiResponse<List<PromptResponseDTO>>> getUserPrompts(
            @RequestHeader("X-User-Id") String userId) {
        try {
            List<PromptResponseDTO> prompts = promptService.getAllPromptsByUser(userId);
            ApiResponse<List<PromptResponseDTO>> response = new ApiResponse<>(
                    "SUCCESS",
                    "Prompts retrieved successfully",
                    prompts
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw e;
        }
    }

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<ApiResponse<List<PromptResponseDTO>>> getProviderPrompts(
            @PathVariable Long providerId) {
        try {
            List<PromptResponseDTO> prompts = promptService.getAllPromptsByProvider(providerId);
            ApiResponse<List<PromptResponseDTO>> response = new ApiResponse<>(
                    "SUCCESS",
                    "Provider prompts retrieved successfully",
                    prompts
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw e;
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deletePrompt(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId) {
        try {
            promptService.deletePrompt(id, userId);
            ApiResponse<Void> response = new ApiResponse<>(
                    "SUCCESS",
                    "Prompt deleted successfully",
                    null
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw e;
        }
    }

    @PostMapping("/{id}/duplicate")
    public ResponseEntity<ApiResponse<PromptResponseDTO>> duplicatePrompt(
            @PathVariable Long id,
            @RequestParam String newName,
            @RequestHeader("X-User-Id") String userId) {
        try {
            PromptResponseDTO prompt = promptService.duplicatePrompt(id, newName, userId);
            ApiResponse<PromptResponseDTO> response = new ApiResponse<>(
                    "SUCCESS",
                    "Prompt duplicated successfully",
                    prompt
            );
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            throw e;
        }
    }

    @PostMapping("/execute")
    public ResponseEntity<ApiResponse<String>> executePrompt(
            @Valid @RequestBody ExecutePromptDTO executeDTO) {
        try {
            String result = promptService.executePrompt(executeDTO);
            ApiResponse<String> response = new ApiResponse<>(
                    "SUCCESS",
                    "Prompt executed successfully",
                    result
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw e;
        }
    }

    @PostMapping("/{id}/reindex")
    public ResponseEntity<ApiResponse<Void>> reindexPrompt(@PathVariable Long id) {
        try {
            promptService.indexPromptInElasticsearch(id);
            ApiResponse<Void> response = new ApiResponse<>(
                    "SUCCESS",
                    "Prompt reindexed successfully",
                    null
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw e;
        }
    }
}

