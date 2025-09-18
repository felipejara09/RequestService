package co.com.bancolombia.api.Errors;

import co.com.bancolombia.usecase.exception.DomainException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.util.Map;

public final class ErrorMapper {
    private ErrorMapper() {}

    public static Mono<ServerResponse> map(Throwable ex) {
        if (ex instanceof DomainException de) {
            HttpStatus status = switch (de.getMessage()) {
                case "LOAN_TYPE_NOT_FOUND" -> HttpStatus.BAD_REQUEST;
                case "AMOUNT_OUT_OF_RANGE" -> HttpStatus.BAD_REQUEST;
                case "CUSTOMER_NOT_VERIFIED" -> HttpStatus.UNPROCESSABLE_ENTITY;
                case "FORBIDDEN" -> HttpStatus.FORBIDDEN;
                case "FORBIDDEN_OTHER_CUSTOMER" -> HttpStatus.FORBIDDEN;
                case "UNAUTHORIZED" -> HttpStatus.UNAUTHORIZED;
                case "INVALID_DECISION" -> HttpStatus.BAD_REQUEST;
                case "APPLICATION_NOT_FOUND" -> HttpStatus.NOT_FOUND;
                default -> HttpStatus.BAD_REQUEST;
            };
            return ServerResponse.status(status)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("code", de.getMessage()));
        }
        return ServerResponse.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("code", "INTERNAL_ERROR"));
    }
}
