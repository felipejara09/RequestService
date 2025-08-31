package co.com.bancolombia.model.loanapplication.gateways;

import reactor.core.publisher.Mono;

public interface CustumerServiceGateway {
    Mono<Boolean> verifyIdentity(String identificationNumber, String email);
}
