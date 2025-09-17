package co.com.bancolombia.model.auth.gateways;

import co.com.bancolombia.model.auth.UserInfo;

import reactor.core.publisher.Mono;

public interface AuthGateway {
    Mono<UserInfo> currentUser(String authorizationHeader);;
}
