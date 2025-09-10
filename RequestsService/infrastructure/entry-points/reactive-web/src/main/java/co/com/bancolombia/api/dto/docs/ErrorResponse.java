package co.com.bancolombia.api.dto.docs;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "ErrorResponse")
public class ErrorResponse {
    public String code;
    public String message;
}
