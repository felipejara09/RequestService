package co.com.bancolombia.usecase.registerloanapplication;


import co.com.bancolombia.model.loanapplication.LoanApplication;
import co.com.bancolombia.model.loanapplication.LoanType;
import co.com.bancolombia.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.model.loanapplication.gateways.LoanTypeRepository;
import co.com.bancolombia.model.loanapplication.gateways.CustumerServiceGateway;
import co.com.bancolombia.usecase.exception.DomainException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@RequiredArgsConstructor
public class RegisterLoanApplicationUseCase {

    private final CustumerServiceGateway customerGateway;
    private final LoanTypeRepository loanTypeRepository;
    private final LoanApplicationRepository loanApplicationRepository;

    public Mono<LoanApplication> execute(LoanApplication cmd) {
        return loanTypeRepository.findById(cmd.getLoanTypeId())
                .switchIfEmpty(Mono.error(new DomainException("LOAN_TYPE_NOT_FOUND")))
                .flatMap(type -> validateAmount(cmd.getAmount(), type)
                        .then(customerGateway.verifyIdentity(cmd.getIdentificationNumber(), cmd.getEmail()))
                        .flatMap(verified -> verified
                                ? persist(cmd, "PENDING_REVIEW")
                                : Mono.error(new DomainException("CUSTOMER_NOT_VERIFIED"))));
    }

    private Mono<Void> validateAmount(BigDecimal amount, LoanType t) {
        boolean ok = amount.compareTo(t.getMinAmount()) >= 0 && amount.compareTo(t.getMaxAmount()) <= 0;
        return ok ? Mono.empty() : Mono.error(new DomainException("AMOUNT_OUT_OF_RANGE"));
    }

    private Mono<LoanApplication> persist(LoanApplication cmd, String status) {
        LoanApplication la = LoanApplication.builder()
                .applicationId (null)
                .identificationNumber(cmd.getIdentificationNumber())
                .email(cmd.getEmail())
                .amount(cmd.getAmount())
                .termMonths (cmd.getTermMonths())
                .loanTypeId(cmd.getLoanTypeId())
                .statusId(cmd.getStatusId())
                .build();
        return loanApplicationRepository.save(la);
    }

}
