package co.com.bancolombia.usecase.registerloanapplication;


import co.com.bancolombia.model.auth.Actor;
import co.com.bancolombia.model.auth.Role;
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


    public Mono<RegisterLoanApplicationResult> execute(LoanApplication cmd, Actor actor) {
        if (actor == null || actor.getRole() == null) {
            return Mono.error(new DomainException("UNAUTHORIZED"));
        }


        final LoanApplication finalCmd;
        if (actor.getRole() == Role.CLIENT) {
            String normalizedEmail = normalize(cmd.getEmail());
            if (!equalsIgnoreCaseTrim(normalizedEmail, actor.getEmail())) {
                return Mono.error(new DomainException("FORBIDDEN_OTHER_CUSTOMER"));
            }
            finalCmd = cmd.toBuilder().email(normalizedEmail).build();
        } else {
            finalCmd = cmd;
        }

        return loanTypeRepository.findById(finalCmd.getLoanTypeId())
                .switchIfEmpty(Mono.error(new DomainException("LOAN_TYPE_NOT_FOUND")))
                .flatMap(type ->
                        validateAmount(finalCmd.getAmount(), type)
                                .then(Mono.defer(() -> customerGateway.verifyIdentity(
                                        finalCmd.getIdentificationNumber(), finalCmd.getEmail())))
                                .flatMap(verified -> verified
                                        ? persist(finalCmd, 1)
                                        : Mono.error(new DomainException("CUSTOMER_NOT_VERIFIED")))
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
                .termMonths(cmd.getTermMonths())
                .loanTypeId(cmd.getLoanTypeId())
                .statusId(statusId)
                .build();
        return loanApplicationRepository.save(la)
                .flatMap(saved ->
                        stateRepository.findNameById(saved.getStatusId())
                                .map(State::getName)
                                .defaultIfEmpty("Pending review")
                                .map(statusName -> new RegisterLoanApplicationResult(
                                        saved.getApplicationId(),
                                        saved.getStatusId(),
                                        statusName
                                )));
    }

    private static String normalize(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }
    private static boolean equalsIgnoreCaseTrim(String a, String b) {
        return a != null && b != null && a.trim().equalsIgnoreCase(b.trim());
    }
}
