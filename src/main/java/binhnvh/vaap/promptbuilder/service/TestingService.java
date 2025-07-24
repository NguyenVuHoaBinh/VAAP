package binhnvh.vaap.promptbuilder.service;

import binhnvh.vaap.promptbuilder.dto.TestPromptDTO;
import binhnvh.vaap.promptbuilder.dto.TestResultDTO;

import java.util.List;

public interface TestingService {
    TestResultDTO createAndRunTest(TestPromptDTO testPromptDTO, String userId);
    TestResultDTO getTestResults(Long testId);
    List<TestResultDTO> getAllTestsByUser(String userId);
    void cancelTest(Long testId, String userId);
    TestResultDTO runAutoOptimization(Long promptId, String userId);
    List<TestResultDTO> getTestHistoryForPrompt(Long promptId);
}
