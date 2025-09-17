package co.com.bancolombia.r2dbc.gateways;

import co.com.bancolombia.r2dbc.entity.StateEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;


public interface StateReactiveRepository
        extends ReactiveCrudRepository<StateEntity, Integer>,
        ReactiveQueryByExampleExecutor<StateEntity> {}


