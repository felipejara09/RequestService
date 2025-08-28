package co.com.bancolombia.r2dbc;

import co.com.bancolombia.r2dbc.entity.LoanApplicationEntity;
import co.com.bancolombia.r2dbc.entity.LoanTypeEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;

public interface LoanTypeReactiveRepository
        extends ReactiveCrudRepository<LoanTypeEntity, Integer>,
        ReactiveQueryByExampleExecutor<LoanTypeEntity>
{

}