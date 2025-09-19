package co.com.bancolombia.model.loanapplication.gateways;

import co.com.bancolombia.model.loanapplication.LoanApplication;
import reactor.core.publisher.Mono;

import java.util.UUID;

public interface LoanApplicationRepository {
    Mono<LoanApplication> save(LoanApplication application);
    Mono<LoanApplication> changeStatus(UUID applicationId, Integer newStatusId);
}
