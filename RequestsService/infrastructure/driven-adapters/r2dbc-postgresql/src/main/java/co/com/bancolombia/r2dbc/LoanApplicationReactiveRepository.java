package co.com.bancolombia.r2dbc;

import co.com.bancolombia.r2dbc.entity.LoanApplicationEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;

// TODO: This file is just an example, you should delete or modify it
public interface LoanApplicationReactiveRepository
        extends ReactiveCrudRepository<LoanApplicationEntity, String>,
        ReactiveQueryByExampleExecutor<LoanApplicationEntity>{

}
