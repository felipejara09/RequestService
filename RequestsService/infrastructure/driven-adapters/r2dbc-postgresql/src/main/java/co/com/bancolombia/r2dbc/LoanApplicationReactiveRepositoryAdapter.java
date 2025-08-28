package co.com.bancolombia.r2dbc;

import co.com.bancolombia.model.loanapplication.LoanApplication;
import co.com.bancolombia.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.model.loanapplication.gateways.LoanTypeRepository;
import co.com.bancolombia.r2dbc.entity.LoanApplicationEntity;
import co.com.bancolombia.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public class LoanApplicationReactiveRepositoryAdapter extends ReactiveAdapterOperations<
        LoanApplication,
        LoanApplicationEntity,
        String,
        LoanApplicationReactiveRepository
        > implements LoanApplicationRepository {

    public LoanApplicationReactiveRepositoryAdapter(LoanApplicationReactiveRepository repository, ObjectMapper mapper) {

        super(repository, mapper, d -> mapper.mapBuilder(d, LoanApplication.LoanApplicationBuilder.class).build());
    }

    @Override
    public Mono<LoanApplication> save(LoanApplication application) {

        return repository.save(mapper.map(application, LoanApplicationEntity.class))
                .map(this::toEntity);
    }
}
