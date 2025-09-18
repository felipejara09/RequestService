package co.com.bancolombia.usecase.decideloan;

import co.com.bancolombia.model.auth.Actor;
import co.com.bancolombia.model.loanapplication.Decision;
import co.com.bancolombia.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.model.loanapplication.gateways.StateRepository;
import co.com.bancolombia.model.notification.gateways.NotificationGateway;
import co.com.bancolombia.usecase.RegisterLoanApplicationResult;
import co.com.bancolombia.usecase.exception.DomainException;
import lombok.RequiredArgsConstructor;

import reactor.core.publisher.Mono;


import java.util.UUID;

@RequiredArgsConstructor
public class DecideLoanApplicationUseCase {

    private final LoanApplicationRepository repository;
    private final StateRepository stateRepository;
    private final NotificationGateway notifier;

    public Mono<RegisterLoanApplicationResult> execute(UUID applicationId, Decision decision, Actor actor) {
        if (actor == null || actor.getRole() == null) return Mono.error(new DomainException("UNAUTHORIZED"));
        if (!actor.getRole().isAdminOrAdvisor()) return Mono.error(new DomainException("FORBIDDEN"));

        int newState = decision.toStateId();

        return repository.changeStatus(applicationId, newState)
                .flatMap(updated ->
                        stateRepository.findNameById(updated.getStatusId())
                                .map(s -> s.getName())
                                .defaultIfEmpty("Updated")
                                .flatMap((String statusName) ->
                                        notifier.publishStatusChange(
                                                new NotificationGateway.StatusChangedEvent(
                                                        updated.getApplicationId(),
                                                        updated.getEmail(),
                                                        updated.getStatusId(),
                                                        statusName
                                                )
                                        ).thenReturn(new RegisterLoanApplicationResult(
                                                updated.getApplicationId(),
                                                updated.getStatusId(),
                                                statusName
                                        ))
                                )
                );
    }
}

