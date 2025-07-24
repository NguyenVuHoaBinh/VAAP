package binhnvh.vaap.promptbuilder.controller;

import binhnvh.vaap.promptbuilder.ApiResponse;
import binhnvh.vaap.promptbuilder.dto.TestPromptDTO;
import binhnvh.vaap.promptbuilder.dto.TestResultDTO;
import binhnvh.vaap.promptbuilder.service.TestingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/tests")
public class TestController {

    @Autowired
    private TestingService testingService;

    @PostMapping
    public ResponseEntity<ApiResponse<TestResultDTO>> createAndRunTest(
            @Valid @RequestBody TestPromptDTO testDTO,
            @RequestHeader("X-User-Id") String userId) {
        try {
            TestResultDTO result = testingService.createAndRunTest(testDTO, userId);
            ApiResponse<TestResultDTO> response = new ApiResponse<>(
                    "SUCCESS",
                    "Test created and started successfully",
                    result
            );
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            throw e;
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<TestResultDTO>> getTestResults(@PathVariable Long id) {
        try {
            TestResultDTO result = testingService.getTestResults(id);
            ApiResponse<TestResultDTO> response = new ApiResponse<>(
                    "SUCCESS",
                    "Test results retrieved successfully",
                    result
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw e;
        }
    }

    @GetMapping("/user")
    public ResponseEntity<ApiResponse<List<TestResultDTO>>> getUserTests(
            @RequestHeader("X-User-Id") String userId) {
        try {
            List<TestResultDTO> tests = testingService.getAllTestsByUser(userId);
            ApiResponse<List<TestResultDTO>> response = new ApiResponse<>(
                    "SUCCESS",
                    "User tests retrieved successfully",
                    tests
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw e;
        }
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelTest(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId) {
        try {
            testingService.cancelTest(id, userId);
            ApiResponse<Void> response = new ApiResponse<>(
                    "SUCCESS",
                    "Test cancelled successfully",
                    null
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw e;
        }
    }

    @PostMapping("/optimize/{promptId}")
    public ResponseEntity<ApiResponse<TestResultDTO>> runAutoOptimization(
            @PathVariable Long promptId,
            @RequestHeader("X-User-Id") String userId) {
        try {
            TestResultDTO result = testingService.runAutoOptimization(promptId, userId);
            ApiResponse<TestResultDTO> response = new ApiResponse<>(
                    "SUCCESS",
                    "Auto-optimization started successfully",
                    result
            );
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (Exception e) {
            throw e;
        }
    }

    @GetMapping("/prompt/{promptId}")
    public ResponseEntity<ApiResponse<List<TestResultDTO>>> getPromptTestHistory(
            @PathVariable Long promptId) {
        try {
            List<TestResultDTO> history = testingService.getTestHistoryForPrompt(promptId);
            ApiResponse<List<TestResultDTO>> response = new ApiResponse<>(
                    "SUCCESS",
                    "Test history retrieved successfully",
                    history
            );
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            throw e;
        }
    }
}

