package binhnvh.vaap.promptbuilder.service;

import java.util.Map;

public interface LLMService {
    String callLLM(Long providerId, String model, String prompt, Map<String, Object> parameters);
    Map<String, Object> testLLMConnection(Long providerId, String apiKey);
    Double estimateCost(Long providerId, String model, String prompt);
    Integer countTokens(String text, String model);
}
