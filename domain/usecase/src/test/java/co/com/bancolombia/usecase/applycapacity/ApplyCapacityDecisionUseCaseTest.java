package co.com.bancolombia.usecase.applycapacity;

import co.com.bancolombia.model.loanapplication.LoanApplication;
import co.com.bancolombia.model.loanapplication.States;
import co.com.bancolombia.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.model.loanapplication.gateways.StateRepository;
import co.com.bancolombia.model.notification.gateways.NotificationGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ApplyCapacityDecisionUseCaseTest {

    private LoanApplicationRepository repository;
    private StateRepository stateRepository;
    private NotificationGateway notifier;
    private ApplyCapacityDecisionUseCase useCase;

    private final UUID appId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        repository = mock(LoanApplicationRepository.class);
        stateRepository = mock(StateRepository.class);
        notifier = mock(NotificationGateway.class);
        useCase = new ApplyCapacityDecisionUseCase(repository, stateRepository, notifier);
    }

    private LoanApplication loan(String email) {
        LoanApplication la = mock(LoanApplication.class);
        when(la.getApplicationId()).thenReturn(appId);
        when(la.getEmail()).thenReturn(email);
        return la;
    }

    @Test
    void approved_happyPath_persists_and_notifies() {

        LoanApplication updated = mock(LoanApplication.class);
        when(updated.getApplicationId()).thenReturn(appId);
        when(updated.getEmail()).thenReturn("client@mail.com");

        when(repository.changeStatus(appId, States.APPROVED)).thenReturn(Mono.just(updated));


        when(stateRepository.findNameById(States.APPROVED)).thenReturn(Mono.empty());

        when(notifier.publishStatusChange(any())).thenReturn(Mono.empty());

        var cmd = new ApplyCapacityDecisionUseCase.Cmd(appId, "APROBADO");

        StepVerifier.create(useCase.execute(cmd))
                .consumeNextWith(res -> {
                    assertEquals(appId, res.applicationId());
                    assertEquals(States.APPROVED, res.statusId());    // <-- compare id with id
                    assertEquals("Updated", res.statusName());        // <-- no name mocked => "Updated"
                })
                .verifyComplete();

        verify(repository).changeStatus(appId, States.APPROVED);
        verify(notifier).publishStatusChange(argThat(matchesEvent(appId, States.APPROVED, "Updated")));
        verifyNoMoreInteractions(notifier);
    }


    @Test
    void rejected_happyPath() {
        LoanApplication updated = mock(LoanApplication.class);
        when(updated.getApplicationId()).thenReturn(appId);
        when(updated.getEmail()).thenReturn("client@mail.com");

        when(repository.changeStatus(appId, States.REJECTED)).thenReturn(Mono.just(updated));
        when(stateRepository.findNameById(States.REJECTED)).thenReturn(Mono.empty());
        when(notifier.publishStatusChange(any())).thenReturn(Mono.empty());

        var cmd = new ApplyCapacityDecisionUseCase.Cmd(appId, "RECHAZADO");

        StepVerifier.create(useCase.execute(cmd))
                .consumeNextWith(res -> {
                    assertEquals(appId, res.applicationId());
                    assertEquals(States.REJECTED, res.statusId());
                    assertEquals("Updated", res.statusName());
                })
                .verifyComplete();

        verify(repository).changeStatus(appId, States.REJECTED);
        verify(notifier).publishStatusChange(argThat(matchesEvent(appId, States.REJECTED, "Updated")));
        verifyNoMoreInteractions(notifier);
    }

    @Test
    void manualReview_happyPath() {
        var updated = loan("y@mail.com");
        when(repository.changeStatus(appId, States.MANUAL_REVIEW)).thenReturn(Mono.just(updated));
        when(stateRepository.findNameById(States.MANUAL_REVIEW)).thenReturn(Mono.empty());
        when(notifier.publishStatusChange(any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(new ApplyCapacityDecisionUseCase.Cmd(appId, "REVISION_MANUAL")))
                .expectNextCount(1)
                .verifyComplete();

        verify(repository).changeStatus(appId, States.MANUAL_REVIEW);
        verify(notifier).publishStatusChange(any());
    }

    @Test
    void notifier_failure_is_ignored() {
        var updated = loan("z@mail.com");
        when(repository.changeStatus(appId, States.APPROVED)).thenReturn(Mono.just(updated));
        when(stateRepository.findNameById(States.APPROVED)).thenReturn(Mono.empty());
        when(notifier.publishStatusChange(any())).thenReturn(Mono.error(new RuntimeException("SNS down")));

        StepVerifier.create(useCase.execute(new ApplyCapacityDecisionUseCase.Cmd(appId, "APROBADO")))
                .expectNextCount(1)
                .verifyComplete();
    }


    private ArgumentMatcher<NotificationGateway.StatusChangedEvent> matchesEvent(UUID id, int statusId, String statusName) {
        return e -> e != null
                && id.equals(e.applicationId())
                && statusId == e.newStatusId()
                && statusName.equals(e.newStatusName());
    }
}