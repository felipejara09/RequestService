package co.com.bancolombia.api;

import co.com.bancolombia.api.Errors.ErrorMapper;
import co.com.bancolombia.api.dto.*;
import co.com.bancolombia.api.segurity.JwtUtils;
import co.com.bancolombia.model.auth.Actor;
import co.com.bancolombia.model.auth.Role;
import co.com.bancolombia.model.loanapplication.Decision;
import co.com.bancolombia.usecase.decideloan.DecideLoanApplicationUseCase;
import co.com.bancolombia.usecase.exception.DomainException;
import co.com.bancolombia.usecase.listmanualreview.ListManualReviewUseCase;
import co.com.bancolombia.usecase.registerloanapplication.RegisterLoanApplicationUseCase;
import co.com.bancolombia.usecase.requestcapacity.RequestCapacityCalculationUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Map;


@Component
@RequiredArgsConstructor
@Slf4j
public class Handler {

    private final RegisterLoanApplicationUseCase registerLoanApplicationUseCase;
    private final ListManualReviewUseCase listManualReviewUseCase;
    private final DecideLoanApplicationUseCase decideLoanApplicationUseCase;
    private final RequestCapacityCalculationUseCase requestCapacityCalculationUseCase;

    public Mono<ServerResponse> register(ServerRequest request) {
        String auth = request.headers().firstHeader(HttpHeaders.AUTHORIZATION);
        Actor actor = actorFromAuth(auth);

        return request.bodyToMono(RegisterLoanApplicationRequest.class)
                .map(RegisterLoanApplicationRequest::toDomain)
                .flatMap(domain -> registerLoanApplicationUseCase.execute(domain, actor))
                .flatMap(res -> ServerResponse.created(URI.create("/api/v1/solicitud/" + res.applicationId()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(new RegisterLoanApplicationResponse(
                                res.applicationId(), res.statusId(), res.statusName()
                        )))
                .onErrorResume(ex -> {
                    log.error("register() failed", ex);
                    return ErrorMapper.map(ex);
                })
                .contextWrite(ctx -> auth != null ? ctx.put("AUTH_TOKEN", auth) : ctx);
    }

    public Mono<ServerResponse> list(ServerRequest request) {
        String auth = request.headers().firstHeader(HttpHeaders.AUTHORIZATION);
        Actor actor = actorFromAuth(auth);

        int page = parseIntSafe(request.queryParam("page").orElse("0"), 0);
        int size = parseIntSafe(request.queryParam("size").orElse("20"), 20);
        String q  = request.queryParam("q").orElse(null);

        return listManualReviewUseCase.execute(page, size, q, actor)
                .flatMap(pageRes -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(pageRes))
                .onErrorResume(ex -> {
                    log.error("list() failed", ex);
                    return ErrorMapper.map(ex);
                })
                .contextWrite(ctx -> auth != null ? ctx.put("AUTH_TOKEN", auth) : ctx);
    }


    private Actor actorFromAuth(String auth) {
        var claims = JwtUtils.decodeClaims(auth);
        Integer roleId = JwtUtils.claimInt(claims, "role");
        String email   = JwtUtils.claimString(claims, "email");
        Integer sub    = JwtUtils.claimInt(claims, "sub"); // si viene
        return Actor.builder()
                .userId(sub)
                .role(Role.from(roleId))
                .email(email)
                .build();
    }
    private int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }

    public Mono<ServerResponse> changeStatus(ServerRequest request) {
        String auth = request.headers().firstHeader(HttpHeaders.AUTHORIZATION);
        Actor actor = actorFromAuth(auth);

        return request.bodyToMono(DecisionRequest.class)
                .flatMap(body ->
                        decideLoanApplicationUseCase.execute(
                                body.applicationId(),
                                Decision.from(body.decision()), actor))
                .flatMap(res -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(new DecisionResponse(
                                res.applicationId(), res.statusId(), res.statusName())))
                .onErrorResume(ex -> {
                    log.error("changeStatus() failed", ex);
                    return ErrorMapper.map(ex);
                })
                .contextWrite(ctx -> auth != null ? ctx.put("AUTH_TOKEN", auth) : ctx);
    }

    public Mono<ServerResponse> requestCapacity(ServerRequest request) {
        String auth = request.headers().firstHeader(HttpHeaders.AUTHORIZATION);
        Actor actor = actorFromAuth(auth);

        return request.bodyToMono(CapacityRequestDto.class)
                .flatMap(b -> requestCapacityCalculationUseCase.execute(
                        new RequestCapacityCalculationUseCase.Cmd(
                                b.applicationId(), b.identificationNumber(), b.email(),
                                b.amount(), b.termMonths(), b.annualInterestRate(),
                                b.monthlyIncome(), b.currentMonthlyDebt()
                        ), actor
                ))
                .then(ServerResponse.accepted()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(Map.of("status","PROCESSING")))
                .onErrorResume(ex -> ErrorMapper.map(ex))
                .contextWrite(ctx -> auth != null ? ctx.put("AUTH_TOKEN", auth) : ctx);
    }
}

