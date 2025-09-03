package co.com.bancolombia.api;

import co.com.bancolombia.api.Errors.ErrorMapper;
import co.com.bancolombia.api.dto.RegisterLoanApplicationRequest;
import co.com.bancolombia.api.dto.RegisterLoanApplicationResponse;
import co.com.bancolombia.usecase.registerloanapplication.RegisterLoanApplicationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;

@Component
@RequiredArgsConstructor
@Slf4j
public class Handler {

    private final RegisterLoanApplicationUseCase useCase;


    public Mono<ServerResponse> register(ServerRequest request) {
        return request
                .bodyToMono(RegisterLoanApplicationRequest.class)
                .map(RegisterLoanApplicationRequest::toDomain)
                .flatMap(useCase::execute)
                .flatMap(res ->
                        ServerResponse.created(URI.create("/api/v1/solicitud/" + res.applicationId()))
                                .contentType(MediaType.APPLICATION_JSON)
                                .bodyValue(new RegisterLoanApplicationResponse(
                                        res.applicationId(),
                                        res.statusId(),
                                        res.statusName()
                                ))
                )
                .onErrorResume(ex -> {
                    log.error("register() failed", ex);
                    return ErrorMapper.map(ex);
                });
    }
}


