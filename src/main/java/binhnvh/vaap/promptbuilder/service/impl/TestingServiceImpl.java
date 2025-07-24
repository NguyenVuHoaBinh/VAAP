package binhnvh.vaap.promptbuilder.service.impl;

import binhnvh.vaap.promptbuilder.dto.ExecutePromptDTO;
import binhnvh.vaap.promptbuilder.dto.TestPromptDTO;
import binhnvh.vaap.promptbuilder.dto.TestResultDTO;
import binhnvh.vaap.promptbuilder.entity.Prompt;
import binhnvh.vaap.promptbuilder.entity.PromptTest;
import binhnvh.vaap.promptbuilder.entity.PromptTestResult;
import binhnvh.vaap.promptbuilder.exception.PromptBuilderException;
import binhnvh.vaap.promptbuilder.repository.PromptRepository;
import binhnvh.vaap.promptbuilder.repository.PromptTestRepository;
import binhnvh.vaap.promptbuilder.repository.PromptTestResultRepository;
import binhnvh.vaap.promptbuilder.repository.PromptVariableRepository;
import binhnvh.vaap.promptbuilder.service.LLMService;
import binhnvh.vaap.promptbuilder.service.PromptService;
import binhnvh.vaap.promptbuilder.service.TestingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class TestingServiceImpl implements TestingService {

    @Autowired
    private PromptTestRepository testRepository;

    @Autowired
    private PromptTestResultRepository resultRepository;

    @Autowired
    private PromptRepository promptRepository;

    @Autowired
    private PromptVariableRepository variableRepository;

    @Autowired
    private PromptService promptService;

    @Autowired
    private LLMService llmService;

    @Override
    @Transactional
    public TestResultDTO createAndRunTest(TestPromptDTO testDTO, String userId) {
        // Validate prompts exist
        Prompt promptA = promptRepository.findByIdAndActiveTrue(testDTO.promptAId())
                .orElseThrow(() -> new PromptBuilderException(
                        PromptBuilderException.ErrorCode.PROMPT_NOT_FOUND,
                        "Prompt A not found"
                ));

        Prompt promptB = null;
        if (testDTO.testType() == PromptTest.TestType.AB_TEST) {
            promptB = promptRepository.findByIdAndActiveTrue(testDTO.promptBId())
                    .orElseThrow(() -> new PromptBuilderException(
                            PromptBuilderException.ErrorCode.PROMPT_NOT_FOUND,
                            "Prompt B not found"
                    ));
        }

        // Create test entity
        PromptTest test = new PromptTest();
        test.setTestName(testDTO.testName());
        test.setTestType(testDTO.testType());
        test.setPromptA(promptA);
        test.setPromptB(promptB);
        test.setSampleSize(testDTO.sampleSize());
        test.setCreatedBy(userId);
        test.setStatus(PromptTest.TestStatus.CREATED);

        PromptTest savedTest = testRepository.save(test);

        // Run test asynchronously
        runTestAsync(savedTest, testDTO.testVariables());

        return convertToTestResultDTO(savedTest);
    }

    @Async("testingExecutor")
    protected void runTestAsync(PromptTest test, Map<String, Object> testVariables) {
        try {
            test.setStatus(PromptTest.TestStatus.RUNNING);
            test.setStartedAt(LocalDateTime.now());
            testRepository.save(test);

            // Run tests for prompt A
            List<PromptTestResult> resultsA = runPromptTests(
                    test,
                    test.getPromptA(),
                    test.getSampleSize(),
                    testVariables
            );

            // Run tests for prompt B if it's an A/B test
            if (test.getTestType() == PromptTest.TestType.AB_TEST && test.getPromptB() != null) {
                List<PromptTestResult> resultsB = runPromptTests(
                        test,
                        test.getPromptB(),
                        test.getSampleSize(),
                        testVariables
                );
                resultsA.addAll(resultsB);
            }

            // Save all results
            resultRepository.saveAll(resultsA);

            // Update test status
            test.setStatus(PromptTest.TestStatus.COMPLETED);
            test.setCompletedAt(LocalDateTime.now());
            testRepository.save(test);

        } catch (Exception e) {
            test.setStatus(PromptTest.TestStatus.FAILED);
            test.setCompletedAt(LocalDateTime.now());
            testRepository.save(test);
            throw new PromptBuilderException(
                    PromptBuilderException.ErrorCode.TEST_EXECUTION_ERROR,
                    "Test execution failed: " + e.getMessage(),
                    e
            );
        }
    }

    private List<PromptTestResult> runPromptTests(PromptTest test, Prompt prompt,
                                                  int sampleSize, Map<String, Object> variables) {
        List<PromptTestResult> results = new ArrayList<>();
        List<Long> executionTimes = new ArrayList<>();
        List<Integer> tokenCounts = new ArrayList<>();
        List<Double> costs = new ArrayList<>();
        int successCount = 0;

        for (int i = 0; i < sampleSize; i++) {
            try {
                long startTime = System.currentTimeMillis();

                // Execute prompt
                ExecutePromptDTO executeDTO = new ExecutePromptDTO(
                        prompt.getId(),
                        variables,
                        false
                );

                String result = promptService.executePrompt(executeDTO);
                long executionTime = System.currentTimeMillis() - startTime;

                // Calculate metrics
                int tokenCount = llmService.countTokens(result, prompt.getModelName());
                double cost = llmService.estimateCost(
                        prompt.getLlmProvider().getId(),
                        prompt.getModelName(),
                        result
                );

                executionTimes.add(executionTime);
                tokenCounts.add(tokenCount);
                costs.add(cost);
                successCount++;

            } catch (Exception e) {
                // Log failure but continue testing
            }
        }

        // Calculate aggregate metrics
        PromptTestResult testResult = new PromptTestResult();
        testResult.setPromptTest(test);
        testResult.setPrompt(prompt);
        testResult.setExecutionTimeMs(calculateAverage(executionTimes));
        testResult.setTokenCount(calculateAverageInt(tokenCounts));
        testResult.setCost(calculateAverageDouble(costs));
        testResult.setSuccessRate((double) successCount / sampleSize * 100);
        testResult.setAverageScore(calculateQualityScore(executionTimes, tokenCounts, costs));

        // Add additional metrics
        Map<String, String> metrics = new HashMap<>();
        metrics.put("min_execution_time", String.valueOf(Collections.min(executionTimes)));
        metrics.put("max_execution_time", String.valueOf(Collections.max(executionTimes)));
        metrics.put("total_cost", String.valueOf(costs.stream().mapToDouble(Double::doubleValue).sum()));
        testResult.setMetrics(metrics);

        results.add(testResult);
        return results;
    }

    @Override
    public TestResultDTO getTestResults(Long testId) {
        PromptTest test = testRepository.findByIdWithDetails(testId)
                .orElseThrow(() -> new PromptBuilderException(
                        PromptBuilderException.ErrorCode.PROMPT_NOT_FOUND,
                        "Test not found"
                ));

        return convertToTestResultDTO(test);
    }

    @Override
    public List<TestResultDTO> getAllTestsByUser(String userId) {
        List<PromptTest> tests = testRepository.findAllByUser(userId);

        return tests.stream()
                .map(this::convertToTestResultDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void cancelTest(Long testId, String userId) {
        PromptTest test = testRepository.findById(testId)
                .orElseThrow(() -> new PromptBuilderException(
                        PromptBuilderException.ErrorCode.PROMPT_NOT_FOUND,
                        "Test not found"
                ));

        if (!test.getCreatedBy().equals(userId)) {
            throw new PromptBuilderException(
                    PromptBuilderException.ErrorCode.PERMISSION_DENIED,
                    "You don't have permission to cancel this test"
            );
        }

        if (test.getStatus() != PromptTest.TestStatus.RUNNING) {
            throw new IllegalArgumentException("Can only cancel running tests");
        }

        test.setStatus(PromptTest.TestStatus.CANCELLED);
        test.setCompletedAt(LocalDateTime.now());
        testRepository.save(test);
    }

    @Override
    @Transactional
    public TestResultDTO runAutoOptimization(Long promptId, String userId) {
        Prompt originalPrompt = promptRepository.findByIdAndActiveTrue(promptId)
                .orElseThrow(() -> new PromptBuilderException(
                        PromptBuilderException.ErrorCode.PROMPT_NOT_FOUND,
                        "Prompt not found"
                ));

        // Generate variations of the prompt
        List<Prompt> variations = generatePromptVariations(originalPrompt, userId);

        // Create a test comparing all variations
        PromptTest test = new PromptTest();
        test.setTestName("Auto-optimization for: " + originalPrompt.getName());
        test.setTestType(PromptTest.TestType.AUTO_OPTIMIZATION);
        test.setPromptA(originalPrompt);
        test.setSampleSize(50); // Smaller sample for auto-optimization
        test.setCreatedBy(userId);
        test.setStatus(PromptTest.TestStatus.CREATED);

        PromptTest savedTest = testRepository.save(test);

        // Run optimization tests
        runOptimizationAsync(savedTest, variations);

        return convertToTestResultDTO(savedTest);
    }

    @Async("testingExecutor")
    protected void runOptimizationAsync(PromptTest test, List<Prompt> variations) {
        try {
            test.setStatus(PromptTest.TestStatus.RUNNING);
            test.setStartedAt(LocalDateTime.now());
            testRepository.save(test);

            // Test original prompt
            List<PromptTestResult> results = runPromptTests(
                    test,
                    test.getPromptA(),
                    test.getSampleSize(),
                    new HashMap<>()
            );

            // Test each variation
            for (Prompt variation : variations) {
                results.addAll(runPromptTests(
                        test,
                        variation,
                        test.getSampleSize(),
                        new HashMap<>()
                ));
            }

            // Save all results
            resultRepository.saveAll(results);

            // Find best performing prompt
            PromptTestResult bestResult = results.stream()
                    .max(Comparator.comparing(PromptTestResult::getAverageScore))
                    .orElse(results.get(0));

            // Add optimization recommendation to metrics
            if (bestResult != null) {
                Map<String, String> metrics = bestResult.getMetrics();
                metrics.put("recommendation", "Best performing prompt: " + bestResult.getPrompt().getName());
                resultRepository.save(bestResult);
            }

            test.setStatus(PromptTest.TestStatus.COMPLETED);
            test.setCompletedAt(LocalDateTime.now());
            testRepository.save(test);

        } catch (Exception e) {
            test.setStatus(PromptTest.TestStatus.FAILED);
            test.setCompletedAt(LocalDateTime.now());
            testRepository.save(test);
            throw new PromptBuilderException(
                    PromptBuilderException.ErrorCode.TEST_EXECUTION_ERROR,
                    "Auto-optimization failed: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public List<TestResultDTO> getTestHistoryForPrompt(Long promptId) {
        List<PromptTest> tests = testRepository.findAllTestsForPrompt(promptId);

        return tests.stream()
                .map(this::convertToTestResultDTO)
                .collect(Collectors.toList());
    }

    private List<Prompt> generatePromptVariations(Prompt original, String userId) {
        List<Prompt> variations = new ArrayList<>();

        // Generate variations with different temperatures, styles, etc.
        // This is a simplified example - in production, you'd use more sophisticated methods

        // Variation 1: More concise
        Prompt concise = duplicatePromptForVariation(original, "Concise", userId);
        concise.setTemplate("Be concise. " + original.getTemplate());
        variations.add(promptRepository.save(concise));

        // Variation 2: More detailed
        Prompt detailed = duplicatePromptForVariation(original, "Detailed", userId);
        detailed.setTemplate("Provide detailed response. " + original.getTemplate());
        variations.add(promptRepository.save(detailed));

        // Variation 3: Different structure
        Prompt structured = duplicatePromptForVariation(original, "Structured", userId);
        structured.setTemplate("Structure your response with clear sections. " + original.getTemplate());
        variations.add(promptRepository.save(structured));

        return variations;
    }

    private Prompt duplicatePromptForVariation(Prompt original, String suffix, String userId) {
        Prompt variation = new Prompt();
        variation.setName(original.getName() + " - " + suffix);
        variation.setDescription("Auto-generated variation: " + suffix);
        variation.setTemplate(original.getTemplate());
        variation.setLlmProvider(original.getLlmProvider());
        variation.setModelName(original.getModelName());
        variation.setCreatedBy(userId);
        variation.setActive(false); // Don't activate variations
        return variation;
    }

    private TestResultDTO convertToTestResultDTO(PromptTest test) {
        List<PromptTestResult> results = test.getResults() != null
                ? new ArrayList<>(test.getResults())
                : resultRepository.findAllByTestId(test.getId());

        List<TestResultDTO.PromptResultDTO> promptResults = results.stream()
                .map(result -> new TestResultDTO.PromptResultDTO(
                        result.getPrompt().getId(),
                        result.getPrompt().getName(),
                        result.getExecutionTimeMs() != null ? result.getExecutionTimeMs().doubleValue() : 0.0,
                        result.getTokenCount() != null ? result.getTokenCount().doubleValue() : 0.0,
                        result.getCost(),
                        result.getSuccessRate(),
                        result.getAverageScore()
                ))
                .collect(Collectors.toList());

        return new TestResultDTO(
                test.getId(),
                test.getTestName(),
                test.getStatus(),
                promptResults,
                test.getStartedAt(),
                test.getCompletedAt()
        );
    }

    private Long calculateAverage(List<Long> values) {
        return values.isEmpty() ? 0L :
                values.stream().mapToLong(Long::longValue).sum() / values.size();
    }

    private Integer calculateAverageInt(List<Integer> values) {
        return values.isEmpty() ? 0 :
                values.stream().mapToInt(Integer::intValue).sum() / values.size();
    }

    private Double calculateAverageDouble(List<Double> values) {
        return values.isEmpty() ? 0.0 :
                values.stream().mapToDouble(Double::doubleValue).sum() / values.size();
    }

    private Double calculateQualityScore(List<Long> executionTimes,
                                         List<Integer> tokenCounts,
                                         List<Double> costs) {
        //TODO: Simple scoring algorithm - in production, this would be more sophisticated
        double timeScore = executionTimes.isEmpty() ? 0 :
                100.0 - (calculateAverage(executionTimes) / 10.0); // Lower time is better
        double tokenScore = tokenCounts.isEmpty() ? 0 :
                100.0 - (calculateAverageInt(tokenCounts) / 10.0); // Fewer tokens is better
        double costScore = costs.isEmpty() ? 0 :
                100.0 - (calculateAverageDouble(costs) * 100); // Lower cost is better

        return (timeScore + tokenScore + costScore) / 3.0;
    }
}
