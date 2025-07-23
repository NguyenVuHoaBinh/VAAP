package binhnvh.vaap.promptbuilder.dto;

public record LLMProviderDTO(
        Long id,
        String providerName,
        String apiEndpoint,
        boolean active,
        Integer rateLimit
) {}
