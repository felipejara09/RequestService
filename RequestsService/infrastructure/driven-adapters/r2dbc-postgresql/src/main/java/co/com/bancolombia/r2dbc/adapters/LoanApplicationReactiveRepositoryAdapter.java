package co.com.bancolombia.r2dbc.adapters;

import co.com.bancolombia.model.loanapplication.LoanApplication;
import co.com.bancolombia.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.r2dbc.entity.LoanApplicationEntity;
import co.com.bancolombia.r2dbc.gateways.LoanApplicationReactiveRepository;
import co.com.bancolombia.r2dbc.gateways.StateReactiveRepository;
import co.com.bancolombia.r2dbc.mappers.LoanApplicationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;


@RequiredArgsConstructor
@Repository
public class LoanApplicationReactiveRepositoryAdapter implements LoanApplicationRepository {

    private final LoanApplicationReactiveRepository repository;
    private final LoanApplicationMapper mapper;
    private final StateReactiveRepository viewRepo;

    @Override
    public Mono<LoanApplication> save(LoanApplication application) {
        LoanApplicationEntity data = mapper.toData(application);
        return repository.save(data).map(mapper::toModel);
    }



}
