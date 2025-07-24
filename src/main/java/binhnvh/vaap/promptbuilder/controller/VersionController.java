package binhnvh.vaap.promptbuilder.controller;

import binhnvh.vaap.promptbuilder.ApiResponse;
import binhnvh.vaap.promptbuilder.dto.PromptVersionDTO;
import binhnvh.vaap.promptbuilder.service.VersionControlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/prompts/{promptId}/versions")
public class VersionController {

    @Autowired
    private VersionControlService versionControlService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<PromptVersionDTO>>> getVersionHistory(
            @PathVariable Long promptId) {
        try {
            List<PromptVersionDTO> versions = versionControlService.getVersionHistory(promptId);
            ApiResponse<List<PromptVersionDTO>> response = new ApiResponse<>(
                    "SUCCESS",
                    "Version history retrieved successfully",
                    versions
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw e;
        }
    }

    @GetMapping("/{versionNumber}")
    public ResponseEntity<ApiResponse<PromptVersionDTO>> getSpecificVersion(
            @PathVariable Long promptId,
            @PathVariable Integer versionNumber) {
        try {
            PromptVersionDTO version = versionControlService.getVersion(promptId, versionNumber);
            ApiResponse<PromptVersionDTO> response = new ApiResponse<>(
                    "SUCCESS",
                    "Version retrieved successfully",
                    version
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw e;
        }
    }

    @PostMapping("/{versionNumber}/rollback")
    public ResponseEntity<ApiResponse<PromptVersionDTO>> rollbackToVersion(
            @PathVariable Long promptId,
            @PathVariable Integer versionNumber,
            @RequestHeader("X-User-Id") String userId) {
        try {
            PromptVersionDTO version = versionControlService.rollbackToVersion(promptId, versionNumber, userId);
            ApiResponse<PromptVersionDTO> response = new ApiResponse<>(
                    "SUCCESS",
                    "Rollback successful",
                    version
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw e;
        }
    }

    @GetMapping("/latest")
    public ResponseEntity<ApiResponse<Integer>> getLatestVersionNumber(
            @PathVariable Long promptId) {
        try {
            Integer latestVersion = versionControlService.getLatestVersionNumber(promptId);
            ApiResponse<Integer> response = new ApiResponse<>(
                    "SUCCESS",
                    "Latest version number retrieved",
                    latestVersion
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw e;
        }
    }
}
