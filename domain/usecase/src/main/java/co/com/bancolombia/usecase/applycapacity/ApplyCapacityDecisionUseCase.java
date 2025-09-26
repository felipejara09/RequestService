package co.com.bancolombia.usecase.applycapacity;

import co.com.bancolombia.model.loanapplication.States;
import co.com.bancolombia.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.model.loanapplication.gateways.StateRepository;
import co.com.bancolombia.model.notification.gateways.NotificationGateway;
import co.com.bancolombia.usecase.RegisterLoanApplicationResult;
import co.com.bancolombia.usecase.exception.DomainException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.util.Locale;
import java.util.UUID;

@RequiredArgsConstructor
public class ApplyCapacityDecisionUseCase {
    private final LoanApplicationRepository repository;
    private final StateRepository stateRepository;
    private final NotificationGateway notifier;

    public record Cmd(UUID applicationId, String finalStatus) {}

    public Mono<RegisterLoanApplicationResult> execute(Cmd cmd) {
        int target = mapStatus(cmd.finalStatus());
        return repository.changeStatus(cmd.applicationId(), target)
                .flatMap(updated -> stateRepository.findNameById(target)
                        .map(s -> s.getName()).defaultIfEmpty("Updated")
                        .flatMap(statusName ->
                                notifier.publishStatusChange(new NotificationGateway.StatusChangedEvent(
                                                updated.getApplicationId(),  target,updated.getAmount()
                                        ))
                                        .onErrorResume(e -> Mono.empty())
                                        .thenReturn(new RegisterLoanApplicationResult(updated.getApplicationId(), target, statusName))
                        )
                );
    }


    public Mono<RegisterLoanApplicationResult> executeSilent(Cmd cmd) {
        int target = mapStatus(cmd.finalStatus());
        return repository.changeStatus(cmd.applicationId(), target)
                .flatMap(updated ->
                        stateRepository.findNameById(target)
                                .map(s -> s.getName()).defaultIfEmpty("Updated")
                                .map(statusName -> new RegisterLoanApplicationResult(
                                        updated.getApplicationId(), target, statusName))
                );
    }


    private int mapStatus(String s) {
        if (s == null) throw new DomainException("INVALID_DECISION");
        String v = s.trim().toUpperCase(Locale.ROOT);
        if (v.startsWith("APROB")) return States.APPROVED;
        if (v.startsWith("RECHAZ")) return States.REJECTED;
        if (v.startsWith("REVISION")) return States.MANUAL_REVIEW;
        throw new DomainException("INVALID_DECISION");
    }
}
