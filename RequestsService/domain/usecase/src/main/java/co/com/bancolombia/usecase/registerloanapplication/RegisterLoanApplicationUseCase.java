package co.com.bancolombia.usecase.registerloanapplication;


import co.com.bancolombia.model.loanapplication.LoanApplication;
import co.com.bancolombia.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.model.loanapplication.gateways.LoanTypeRepository;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RequiredArgsConstructor
public class RegisterLoanApplicationUseCase {

    private final LoanApplicationRepository applicationRepository;
    private final LoanTypeRepository loanTypeRepository;


    public static final int PENDING_REVIEW_STATUS_ID = 1;

    public Mono<LoanApplication> execute(LoanApplication input) {
        return loanTypeRepository.findById(input.getLoanTypeId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Loan type not found")))
                .flatMap(lt -> {
                    var amount = input.getAmount();
                    if (amount.compareTo(lt.getMinAmount()) < 0 || amount.compareTo(lt.getMaxAmount()) > 0) {
                        return Mono.error(new IllegalArgumentException("Amount out of allowed range"));
                    }
                    var enriched = input.toBuilder()
                            .id(UUID.randomUUID().toString())
                            .statusId(PENDING_REVIEW_STATUS_ID)
                            .build();
                    return applicationRepository.save(enriched);
                });
    }
}
