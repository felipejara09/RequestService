package co.com.bancolombia.usecase.registerloanapplication;


import co.com.bancolombia.model.loanapplication.LoanApplication;
import co.com.bancolombia.model.loanapplication.LoanType;
import co.com.bancolombia.model.loanapplication.State;
import co.com.bancolombia.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.model.loanapplication.gateways.LoanTypeRepository;
import co.com.bancolombia.model.loanapplication.gateways.CustumerServiceGateway;
import co.com.bancolombia.model.loanapplication.gateways.StateRepository;
import co.com.bancolombia.usecase.RegisterLoanApplicationResult;
import co.com.bancolombia.usecase.exception.DomainException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;


import java.math.BigDecimal;


@RequiredArgsConstructor
public class RegisterLoanApplicationUseCase {

    private final CustumerServiceGateway customerGateway;
    private final LoanTypeRepository loanTypeRepository;
    private final LoanApplicationRepository loanApplicationRepository;
    private final StateRepository stateRepository;


    public Mono<RegisterLoanApplicationResult> execute(LoanApplication cmd) {
        return loanTypeRepository.findById(cmd.getLoanTypeId())
                .log("RegisterLoanApplicationUseCase.findById")             // INFO por defecto
                .switchIfEmpty(Mono.error(new DomainException("LOAN_TYPE_NOT_FOUND")))
                .flatMap(type ->
                        validateAmount(cmd.getAmount(), type)
                                .log("RegisterLoanApplicationUseCase.validateAmount")
                                .then(customerGateway.verifyIdentity(cmd.getIdentificationNumber(), cmd.getEmail()))
                                .log("RegisterLoanApplicationUseCase.verifyIdentity")
                                .flatMap(verified -> verified
                                        ? persist(cmd, 1).log("RegisterLoanApplicationUseCase.persist")
                                        : Mono.error(new DomainException("CUSTOMER_NOT_VERIFIED"))
                                )
                );
    }

    private Mono<Void> validateAmount(BigDecimal amount, LoanType t) {
        boolean ok = amount.compareTo(t.getMinAmount()) >= 0 && amount.compareTo(t.getMaxAmount()) <= 0;
        return ok ? Mono.empty() : Mono.error(new DomainException("AMOUNT_OUT_OF_RANGE"));
    }

    private Mono<RegisterLoanApplicationResult> persist(LoanApplication cmd, Integer statusId) {
        LoanApplication la = LoanApplication.builder()
                .identificationNumber(cmd.getIdentificationNumber())
                .email(cmd.getEmail())
                .amount(cmd.getAmount())
                .termMonths (cmd.getTermMonths())
                .loanTypeId(cmd.getLoanTypeId())
                .statusId(statusId)
                .build();
        return loanApplicationRepository.save(la)
                .flatMap(saved ->
                        stateRepository.findNameById(saved.getStatusId())
                                .map(State::getName)
                                .defaultIfEmpty("Pending review")
                                .map(statusName  -> new RegisterLoanApplicationResult(
                                        saved.getApplicationId(),
                                        saved.getStatusId(),
                                        statusName
                                ))
                );
    }

}
