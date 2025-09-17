package co.com.bancolombia.model.loanapplication.gateways;

import co.com.bancolombia.model.loanapplication.State;
import reactor.core.publisher.Mono;


public interface StateRepository {
    Mono<State> findNameById(Integer id);
}
