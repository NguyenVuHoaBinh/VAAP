package binhnvh.vaap.promptbuilder.service.impl;

import binhnvh.vaap.promptbuilder.entity.LLMProvider;
import binhnvh.vaap.promptbuilder.exception.PromptBuilderException;
import binhnvh.vaap.promptbuilder.repository.LLMProviderRepository;
import binhnvh.vaap.promptbuilder.service.LLMService;
import binhnvh.vaap.promptbuilder.service.VaultService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Bucket4j;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class LLMServiceImpl implements LLMService {

    @Autowired
    private LLMProviderRepository providerRepository;

    @Autowired
    private VaultService vaultService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    // Rate limiting buckets per provider
    private final Map<Long, Bucket> rateLimitBuckets = new ConcurrentHashMap<>();

    // Token counting cache
    private final Map<String, Integer> tokenCountCache = new ConcurrentHashMap<>();

    @Override
    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, multiplier = 2))
    //TODO: Rework callLLM
    public String callLLM(Long providerId, String model, String prompt, Map<String, Object> parameters) {
        LLMProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new PromptBuilderException(
                        PromptBuilderException.ErrorCode.PROVIDER_NOT_FOUND,
                        "LLM Provider not found"
                ));

        // Check rate limit
        Bucket bucket = getRateLimitBucket(provider);
        if (!bucket.tryConsume(1)) {
            throw new PromptBuilderException(
                    PromptBuilderException.ErrorCode.LLM_API_ERROR,
                    "Rate limit exceeded for provider: " + provider.getProviderName()
            );
        }

        // Get API key from Vault
        String apiKey = vaultService.retrieveApiKey(provider.getVaultPath());

        // Call appropriate provider
        return switch (provider.getProviderName()) {
            case "OPENAI" -> callOpenAI(provider, model, prompt, parameters, apiKey);
            case "ANTHROPIC" -> callAnthropic(provider, model, prompt, parameters, apiKey);
            case "COHERE" -> callCohere(provider, model, prompt, parameters, apiKey);
            default -> throw new PromptBuilderException(
                    PromptBuilderException.ErrorCode.PROVIDER_NOT_FOUND,
                    "Unsupported provider: " + provider.getProviderName()
            );
        };
    }

    //TODO: Rework OpenAI calling
    private String callOpenAI(LLMProvider provider, String model, String prompt,
                              Map<String, Object> parameters, String apiKey) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> request = new HashMap<>();
            request.put("model", model);
            request.put("messages", List.of(
                    Map.of("role", "system", "content", "You are a helpful assistant."),
                    Map.of("role", "user", "content", prompt)
            ));

            // Add parameters
            if (parameters != null) {
                request.putAll(parameters);
            }

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    provider.getApiEndpoint() + "/chat/completions",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    return (String) message.get("content");
                }
            }

            throw new PromptBuilderException(
                    PromptBuilderException.ErrorCode.LLM_API_ERROR,
                    "Invalid response from OpenAI"
            );

        } catch (HttpClientErrorException e) {
            throw new PromptBuilderException(
                    PromptBuilderException.ErrorCode.LLM_API_ERROR,
                    "OpenAI API error: " + e.getMessage(),
                    e
            );
        }
    }

    private String callAnthropic(LLMProvider provider, String model, String prompt,
                                 Map<String, Object> parameters, String apiKey) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("x-api-key", apiKey);
            headers.set("anthropic-version", "2023-06-01");

            Map<String, Object> request = new HashMap<>();
            request.put("model", model);
            request.put("messages", List.of(
                    Map.of("role", "user", "content", prompt)
            ));
            request.put("max_tokens", parameters.getOrDefault("max_tokens", 1000));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    provider.getApiEndpoint() + "/messages",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> content = (List<Map<String, Object>>) body.get("content");
                if (content != null && !content.isEmpty()) {
                    return (String) content.get(0).get("text");
                }
            }

            throw new PromptBuilderException(
                    PromptBuilderException.ErrorCode.LLM_API_ERROR,
                    "Invalid response from Anthropic"
            );

        } catch (HttpClientErrorException e) {
            throw new PromptBuilderException(
                    PromptBuilderException.ErrorCode.LLM_API_ERROR,
                    "Anthropic API error: " + e.getMessage(),
                    e
            );
        }
    }

    private String callCohere(LLMProvider provider, String model, String prompt,
                              Map<String, Object> parameters, String apiKey) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> request = new HashMap<>();
            request.put("prompt", prompt);
            request.put("model", model);
            request.put("max_tokens", parameters.getOrDefault("max_tokens", 1000));
            request.put("temperature", parameters.getOrDefault("temperature", 0.7));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    provider.getApiEndpoint() + "/generate",
                    HttpMethod.POST,
                    entity,
                    Map.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> generations = (List<Map<String, Object>>) body.get("generations");
                if (generations != null && !generations.isEmpty()) {
                    return (String) generations.get(0).get("text");
                }
            }

            throw new PromptBuilderException(
                    PromptBuilderException.ErrorCode.LLM_API_ERROR,
                    "Invalid response from Cohere"
            );

        } catch (HttpClientErrorException e) {
            throw new PromptBuilderException(
                    PromptBuilderException.ErrorCode.LLM_API_ERROR,
                    "Cohere API error: " + e.getMessage(),
                    e
            );
        }
    }

    @Override
    public Map<String, Object> testLLMConnection(Long providerId, String apiKey) {
        LLMProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new PromptBuilderException(
                        PromptBuilderException.ErrorCode.PROVIDER_NOT_FOUND,
                        "LLM Provider not found"
                ));

        Map<String, Object> result = new HashMap<>();

        try {
            // Test with a simple prompt
            String testPrompt = "Hello, please respond with 'Connection successful'.";
            Map<String, Object> params = Map.of("max_tokens", 10, "temperature", 0.1);

            // Temporarily store the API key in vault for testing
            String tempPath = provider.getVaultPath() + "-test-" + System.currentTimeMillis();
            vaultService.storeApiKey(tempPath, apiKey);

            try {
                String response = callLLM(providerId, getDefaultModel(provider), testPrompt, params);
                result.put("status", "success");
                result.put("response", response);
                result.put("provider", provider.getProviderName());
                result.put("endpoint", provider.getApiEndpoint());
            } finally {
                // Clean up test API key
                vaultService.deleteApiKey(tempPath);
            }

        } catch (Exception e) {
            result.put("status", "failed");
            result.put("error", e.getMessage());
            result.put("provider", provider.getProviderName());
        }

        return result;
    }

    @Override
    @Cacheable(value = "tokenCosts", key = "#providerId + '-' + #model + '-' + #prompt.hashCode()")
    public Double estimateCost(Long providerId, String model, String prompt) {
        LLMProvider provider = providerRepository.findById(providerId)
                .orElseThrow(() -> new PromptBuilderException(
                        PromptBuilderException.ErrorCode.PROVIDER_NOT_FOUND,
                        "LLM Provider not found"
                ));

        int tokenCount = countTokens(prompt, model);

        //TODO: Cost per 1K tokens (simplified - in production, this would be in a database)
        Map<String, Double> costPerThousandTokens = Map.of(
                "OPENAI-gpt-4", 0.03,
                "OPENAI-gpt-3.5-turbo", 0.002,
                "ANTHROPIC-claude-3-opus", 0.015,
                "ANTHROPIC-claude-3-sonnet", 0.003,
                "COHERE-command", 0.0015
        );

        String key = provider.getProviderName() + "-" + model;
        Double costPer1K = costPerThousandTokens.getOrDefault(key, 0.01);

        return (tokenCount / 1000.0) * costPer1K;
    }

    //TODO: 1. Extract token from response, 2. Update the calculation in database
    @Override
    public Integer countTokens(String text, String model) {
        if (text == null || text.isEmpty()) {
            return 0;
        }

        // Check cache first
        String cacheKey = model + "-" + text.hashCode();
        Integer cachedCount = tokenCountCache.get(cacheKey);
        if (cachedCount != null) {
            return cachedCount;
        }

        // Simplified token counting - in production, use proper tokenizer
        int tokenCount;
        if (model.contains("gpt")) {
            // GPT models: roughly 1 token per 4 characters
            tokenCount = text.length() / 4;
        } else if (model.contains("gemini")) {
            // Claude models: roughly 1 token per 3.5 characters
            tokenCount = (int) (text.length() / 3.5);
        } else {
            // Default: 1 token per 4 characters
            tokenCount = text.length() / 4;
        }

        // Cache the result
        tokenCountCache.put(cacheKey, tokenCount);

        return tokenCount;
    }

    private Bucket getRateLimitBucket(LLMProvider provider) {
        return rateLimitBuckets.computeIfAbsent(provider.getId(), id -> {
            int rateLimit = provider.getRateLimit() != null ? provider.getRateLimit() : 100;
            Bandwidth limit = Bandwidth.classic(
                    rateLimit,
                    Refill.intervally(rateLimit, Duration.ofMinutes(1))
            );
            return Bucket4j.builder()
                    .addLimit(limit)
                    .build();
        });
    }

    //TODO: Modify the Provider
    private String getDefaultModel(LLMProvider provider) {
        return switch (provider.getProviderName()) {
            case "OPENAI" -> "gpt-4.1";
            case "ANTHROPIC" -> "claude-3-sonnet-20240229";
            case "COHERE" -> "command";
            case "GEMINI" -> "gemini-2.5-flash";
            case "DEEPSEEK" -> "deepseek-chat";
            default -> "default";
        };
    }
}
