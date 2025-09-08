package co.com.bancolombia.api;

import co.com.bancolombia.api.Errors.ErrorMapper;
import co.com.bancolombia.api.dto.RegisterLoanApplicationRequest;
import co.com.bancolombia.api.dto.RegisterLoanApplicationResponse;
import co.com.bancolombia.api.segurity.JwtUtils;
import co.com.bancolombia.usecase.listmanualreview.ListManualReviewUseCase;
import co.com.bancolombia.usecase.registerloanapplication.RegisterLoanApplicationUseCase;
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

    public Mono<ServerResponse> register(ServerRequest request) {
        String auth = request.headers().firstHeader(HttpHeaders.AUTHORIZATION);

        return request
                .bodyToMono(RegisterLoanApplicationRequest.class)
                .flatMap(reqDto -> {
                    var claims = JwtUtils.decodeClaims(auth);
                    Integer roleId = JwtUtils.claimInt(claims, "role");
                    String tokenEmail = JwtUtils.claimString(claims, "email");


                    if (Integer.valueOf(3).equals(roleId)) {
                        if (!equalsIgnoreCaseTrim(reqDto.email(), tokenEmail)) {
                            return ServerResponse.status(403)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(Map.of("code", "FORBIDDEN_OTHER_CUSTOMER",
                                            "message", "You can only create requests for yourself."));
                        }
                    }

                    var domain = reqDto.toDomain().toBuilder()
                            .email(reqDto.email() != null ? reqDto.email().trim().toLowerCase() : null)
                            .build();

                    return registerLoanApplicationUseCase.execute(domain)
                            .flatMap(res -> ServerResponse.created(URI.create("/api/v1/solicitud/" + res.applicationId()))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .bodyValue(new RegisterLoanApplicationResponse(
                                            res.applicationId(), res.statusId(), res.statusName()
                                    )));
                })
                .onErrorResume(ex -> {
                    log.error("register() failed", ex);
                    return ErrorMapper.map(ex);
                })

                .contextWrite(ctx -> auth != null ? ctx.put("AUTH_TOKEN", auth) : ctx);
    }

    private static boolean equalsIgnoreCaseTrim(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }

    public Mono<ServerResponse> list(ServerRequest request) {
        String auth = request.headers().firstHeader(HttpHeaders.AUTHORIZATION);

        int page = parseIntSafe(request.queryParam("page").orElse("0"), 0);
        int size = parseIntSafe(request.queryParam("size").orElse("20"), 20);
        String q  = request.queryParam("q").orElse(null);

        var claims = JwtUtils.decodeClaims(auth);
        Integer role = JwtUtils.claimInt(claims, "role");

        if (role == null || !(role == 2 || role == 1)) {
            return ServerResponse.status(403)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of("code", "FORBIDDEN", "message", "This list can only be viewed by Advisors or Admins"));
        }

        return listManualReviewUseCase.execute(page, size, q)
                .flatMap(pageRes -> ServerResponse.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(pageRes))
                .onErrorResume(ex -> {
                    log.error("list() failed", ex);
                    return ErrorMapper.map(ex);
                })
                .contextWrite(ctx -> auth != null ? ctx.put("AUTH_TOKEN", auth) : ctx);
    }

    private int parseIntSafe(String s, int def) {
        try { return Integer.parseInt(s); } catch (Exception e) { return def; }
    }
}

