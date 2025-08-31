package co.com.bancolombia.r2dbc;

import co.com.bancolombia.model.loanapplication.LoanApplication;
import co.com.bancolombia.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.r2dbc.entity.LoanApplicationEntity;
import co.com.bancolombia.r2dbc.helper.ReactiveAdapterOperations;
import co.com.bancolombia.r2dbc.mappers.LoanApplicationMapper;
import lombok.RequiredArgsConstructor;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;


@RequiredArgsConstructor
@Repository
public class LoanApplicationReactiveRepositoryAdapter implements LoanApplicationRepository {

    private final LoanApplicationReactiveRepository repository;
    private final LoanApplicationMapper mapper;

    @Override
    public Mono<LoanApplication> save(LoanApplication application) {
        LoanApplicationEntity data = mapper.toData(application);
        return repository.save(data).map(mapper::toModel);
    }


}
