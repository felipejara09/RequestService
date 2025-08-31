package co.com.bancolombia.consumer;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import co.com.bancolombia.model.loanapplication.gateways.CustumerServiceGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RestConsumer implements CustumerServiceGateway {

    @Qualifier("customerClient")
    private final WebClient client;

    @Override
    public Mono<Boolean> verifyIdentity(String identificationNumber, String email) {
        return client.get()
                .uri(uri -> uri.path("/api/v1/usuarios/verify-email")
                        .queryParam("email", email)
                        .build())
                .exchangeToMono(resp -> {
                    if (resp.statusCode().is2xxSuccessful()) return Mono.just(true);
                    if (resp.statusCode().value() == 404)    return Mono.just(false);
                    if (resp.statusCode().is4xxClientError()) {
                        return resp.bodyToMono(String.class).defaultIfEmpty("")
                                .doOnNext(b -> log.warn("verifyByEmail 4xx: status={}, body={}", resp.statusCode(), b))
                                .thenReturn(false);
                    }
                    return resp.createException().flatMap(Mono::error);
                })
                .timeout(java.time.Duration.ofSeconds(5));
    }
    private Mono<Boolean> doGetVerify(String id, String email) {
        return client.get()
                .uri(uri -> uri.path("/api/v1/usuarios")
                        .queryParam("identityNumber", id)
                        .queryParam("email", email)
                        .build())
                .exchangeToMono(resp -> {
                    if (resp.statusCode().is2xxSuccessful()) {

                        return Mono.just(true);
                    }
                    if (resp.statusCode() == HttpStatus.NOT_FOUND) {

                        return Mono.just(false);
                    }
                    if (resp.statusCode().is4xxClientError()) {

                        return resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .doOnNext(body -> log.warn("GET verify 4xx: status={}, body={}", resp.statusCode(), body))
                                .thenReturn(false);
                    }

                    return resp.createException().flatMap(Mono::error);
                })
                .timeout(Duration.ofSeconds(3));
    }

    private Mono<Boolean> doPostVerify(String id, String email) {
        var payload = Map.of("identificationNumber", id, "email", email);
        return client.post()
                .uri("/api/v1/usuarios/verify") // <-- ajusta a tu endpoint real si es distinto
                .bodyValue(payload)
                .exchangeToMono(resp -> {
                    if (resp.statusCode().is2xxSuccessful()) return Mono.just(true);
                    if (resp.statusCode() == HttpStatus.NOT_FOUND) return Mono.just(false);
                    if (resp.statusCode().is4xxClientError()) {
                        return resp.bodyToMono(String.class)
                                .defaultIfEmpty("")
                                .doOnNext(body -> log.warn("POST verify 4xx: status={}, body={}", resp.statusCode(), body))
                                .thenReturn(false);
                    }
                    return resp.createException().flatMap(Mono::error);
                })
                .timeout(Duration.ofSeconds(3));
    }

    /*@CircuitBreaker(name = "AutheticationService", fallbackMethod = "verifyFallback")
    public Mono<Boolean> verifyIdentity(String id, String email) {
        return webClient.post()
                .uri("/api/v1/customers/verify")
                .bodyValue(Map.of("identificationNumber", id, "email", email))
                .retrieve()
                .bodyToMono(VerifyResponse.class)
                .map(VerifyResponse::isVerified)
                .timeout(Duration.ofSeconds(3)); // combina con timeout
    }

    private Mono<Boolean> verifyFallback(String id, String email, Throwable ex) {
        return Mono.error(new DomainException("CUSTOMER_NOT_VERIFIED"));
    }*/


}


