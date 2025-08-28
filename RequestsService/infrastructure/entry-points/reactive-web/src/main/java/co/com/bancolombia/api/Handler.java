package co.com.bancolombia.api;

import co.com.bancolombia.api.dto.RegisterLoanApplicationRequest;
import co.com.bancolombia.model.loanapplication.LoanApplication;
import co.com.bancolombia.usecase.registerloanapplication.RegisterLoanApplicationUseCase;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor

public class Handler {
private final RegisterLoanApplicationUseCase useCase;
private final Validator validator;

public Mono<ServerResponse> register(ServerRequest request) {
    return request.bodyToMono(RegisterLoanApplicationRequest.class)
            .flatMap(dto -> {
                var violations = validator.validate(dto);
                if (!violations.isEmpty()) {
                    return Mono.error(new IllegalArgumentException(
                            violations.iterator().next().getMessage()));
                }
                var model = LoanApplication.builder()
                        .amount(dto.getAmount())
                        .termMonths(dto.getTermMonths())
                        .email(dto.getEmail())
                        .identificationNumber(dto.getIdentificationNumber())
                        .loanTypeId(dto.getLoanTypeId())
                        .build();
                return useCase.execute(model);
            })
            .flatMap(saved -> ServerResponse.ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(saved));
}
}

