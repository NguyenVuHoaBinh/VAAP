package binhnvh.vaap.promptbuilder.exception;

public class PromptBuilderException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String details;

    public PromptBuilderException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.details = null;
    }

    public PromptBuilderException(ErrorCode errorCode, String message, String details) {
        super(message);
        this.errorCode = errorCode;
        this.details = details;
    }

    public PromptBuilderException(ErrorCode errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.details = null;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getDetails() {
        return details;
    }

    public enum ErrorCode {
        PROMPT_NOT_FOUND("PB001", "Prompt not found"),
        PROMPT_NAME_EXISTS("PB002", "Prompt name already exists"),
        INVALID_TEMPLATE("PB003", "Invalid prompt template"),
        PROVIDER_NOT_FOUND("PB004", "LLM provider not found"),
        VAULT_ERROR("PB005", "Vault operation failed"),
        ELASTICSEARCH_ERROR("PB006", "Elasticsearch operation failed"),
        LLM_API_ERROR("PB007", "LLM API call failed"),
        TEST_EXECUTION_ERROR("PB008", "Test execution failed"),
        PERMISSION_DENIED("PB009", "Permission denied"),
        INVALID_VARIABLE("PB010", "Invalid variable configuration");

        private final String code;
        private final String defaultMessage;

        ErrorCode(String code, String defaultMessage) {
            this.code = code;
            this.defaultMessage = defaultMessage;
        }

        public String getCode() {
            return code;
        }

        public String getDefaultMessage() {
            return defaultMessage;
        }
    }
}
