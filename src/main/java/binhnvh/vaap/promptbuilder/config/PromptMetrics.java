package binhnvh.vaap.promptbuilder.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

@Component
public class PromptMetrics {

    private final Counter promptExecutions;
    private final Counter testRuns;
    private final Counter llmApiCalls;
    private final Counter vaultOperations;
    private final Timer promptExecutionTimer;
    private final Timer llmApiTimer;

    public PromptMetrics(MeterRegistry registry) {
        this.promptExecutions = Counter.builder("prompt.executions.total")
                .description("Total number of prompt executions")
                .register(registry);

        this.testRuns = Counter.builder("test.runs.total")
                .description("Total number of test runs")
                .register(registry);

        this.llmApiCalls = Counter.builder("llm.api.calls.total")
                .description("Total number of LLM API calls")
                .register(registry);

        this.vaultOperations = Counter.builder("vault.operations.total")
                .description("Total number of Vault operations")
                .register(registry);

        this.promptExecutionTimer = Timer.builder("prompt.execution.duration")
                .description("Prompt execution duration")
                .register(registry);

        this.llmApiTimer = Timer.builder("llm.api.duration")
                .description("LLM API call duration")
                .register(registry);
    }

    public void recordPromptExecution() {
        promptExecutions.increment();
    }

    public void recordTestRun() {
        testRuns.increment();
    }

    public void recordLLMApiCall() {
        llmApiCalls.increment();
    }

    public void recordVaultOperation() {
        vaultOperations.increment();
    }

    public Timer.Sample startPromptExecutionTimer() {
        return Timer.start();
    }

    public void stopPromptExecutionTimer(Timer.Sample sample) {
        sample.stop(promptExecutionTimer);
    }

    public Timer.Sample startLLMApiTimer() {
        return Timer.start();
    }

    public void stopLLMApiTimer(Timer.Sample sample) {
        sample.stop(llmApiTimer);
    }
}