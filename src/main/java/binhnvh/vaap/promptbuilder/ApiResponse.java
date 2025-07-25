package binhnvh.vaap.promptbuilder;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    private String result;    // SUCCESS or ERROR
    private String message;   // success or error message
    private T data;           // return object from service class, if successful

    // Convenience factory methods
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>("SUCCESS", "Operation completed successfully", data);
    }

    public static <T> ApiResponse<T> success(String message, T data) {
        return new ApiResponse<>("SUCCESS", message, data);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>("ERROR", message, null);
    }

    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>("ERROR", String.format("[%d] %s", code, message), null);
    }
}
