package co.com.bancolombia.r2dbc.adapters;

import co.com.bancolombia.model.loanapplication.State;
import co.com.bancolombia.model.loanapplication.gateways.StateRepository;
import co.com.bancolombia.r2dbc.entity.StateEntity;
import co.com.bancolombia.r2dbc.gateways.StateReactiveRepository;
import co.com.bancolombia.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class StateReactiveRepositoryAdapter
        extends ReactiveAdapterOperations<State, StateEntity, Integer, StateReactiveRepository>
        implements StateRepository {

    public StateReactiveRepositoryAdapter(StateReactiveRepository repository, ObjectMapper mapper) {
        super(repository, mapper, d -> mapper.mapBuilder(d, State.StateBuilder.class).build());
    }

    @Override
    public Mono<State> findNameById(Integer id) {
        return repository.findById(id).map(this::toEntity);
    }
}
