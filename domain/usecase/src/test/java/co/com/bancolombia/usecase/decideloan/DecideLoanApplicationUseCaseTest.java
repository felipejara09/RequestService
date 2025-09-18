package co.com.bancolombia.usecase.decideloan;

import co.com.bancolombia.model.auth.Actor;
import co.com.bancolombia.model.auth.Role;
import co.com.bancolombia.model.loanapplication.Decision;
import co.com.bancolombia.model.loanapplication.LoanApplication;
import co.com.bancolombia.model.loanapplication.State;
import co.com.bancolombia.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.model.loanapplication.gateways.StateRepository;
import co.com.bancolombia.model.notification.gateways.NotificationGateway;
import co.com.bancolombia.usecase.RegisterLoanApplicationResult;
import co.com.bancolombia.usecase.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DecideLoanApplicationUseCaseTest {

    @Mock
    private LoanApplicationRepository repository;

    @Mock
    private StateRepository stateRepository;

    @Mock
    private NotificationGateway notifier;

    @Captor
    private ArgumentCaptor<NotificationGateway.StatusChangedEvent> eventCaptor;

    private DecideLoanApplicationUseCase useCase;

    private final UUID appId = UUID.randomUUID();

    private Actor admin() {
        return Actor.builder().userId(1).role(Role.ADMIN).email("admin@bank.com").build();
    }
    private Actor advisor() {
        return Actor.builder().userId(2).role(Role.ADVISOR).email("advisor@bank.com").build();
    }
    private Actor client() {
        return Actor.builder().userId(3).role(Role.CLIENT).email("c@x.com").build();
    }

    @BeforeEach
    void setUp() {
        useCase = new DecideLoanApplicationUseCase(repository, stateRepository, notifier);
    }

    @Nested
    class Guards {

        @Test
        @DisplayName("UNAUTHORIZED cuando actor es null")
        void unauthorizedWhenActorNull() {
            StepVerifier.create(useCase.execute(appId, Decision.APPROVED, null))
                    .expectErrorSatisfies(ex -> {
                        assert ex instanceof DomainException;
                        assert "UNAUTHORIZED".equals(ex.getMessage());
                    })
                    .verify();

            verifyNoInteractions(repository, stateRepository, notifier);
        }

        @Test
        @DisplayName("UNAUTHORIZED cuando el rol del actor es null")
        void unauthorizedWhenRoleNull() {
            Actor noRole = Actor.builder().userId(9).role(null).email("n/a").build();

            StepVerifier.create(useCase.execute(appId, Decision.APPROVED, noRole))
                    .expectErrorSatisfies(ex -> {
                        assert ex instanceof DomainException;
                        assert "UNAUTHORIZED".equals(ex.getMessage());
                    })
                    .verify();

            verifyNoInteractions(repository, stateRepository, notifier);
        }

        @Test
        @DisplayName("FORBIDDEN cuando el actor es CLIENT")
        void forbiddenForClient() {
            StepVerifier.create(useCase.execute(appId, Decision.APPROVED, client()))
                    .expectErrorSatisfies(ex -> {
                        assert ex instanceof DomainException;
                        assert "FORBIDDEN".equals(ex.getMessage());
                    })
                    .verify();

            verifyNoInteractions(repository, stateRepository, notifier);
        }
    }

    @Nested
    class HappyPath {

        @Test
        @DisplayName("Aprueba: con nombre de estado encontrado")
        void approveWithStateName() {
            // given
            int newState = Decision.APPROVED.toStateId(); // el enum mapea al id
            LoanApplication updated = LoanApplication.builder()
                    .applicationId(appId)
                    .email("user@mail.com")
                    .statusId(newState)
                    .build();

            when(repository.changeStatus(eq(appId), eq(newState)))
                    .thenReturn(Mono.just(updated));

            when(stateRepository.findNameById(eq(newState)))
                    .thenReturn(Mono.just(State.builder().stateId(newState).name("Approved").build()));

            when(notifier.publishStatusChange(any())).thenReturn(Mono.empty());

            // when
            Mono<RegisterLoanApplicationResult> mono =
                    useCase.execute(appId, Decision.APPROVED, admin());

            // then
            StepVerifier.create(mono)
                    .assertNext(res -> {
                        assert res.applicationId().equals(appId);
                        assert res.statusId().equals(newState);
                        assert "Approved".equals(res.statusName());
                    })
                    .verifyComplete();

            verify(repository).changeStatus(appId, newState);
            verify(stateRepository).findNameById(newState);
            verify(notifier).publishStatusChange(eventCaptor.capture());

            NotificationGateway.StatusChangedEvent evt = eventCaptor.getValue();
            assert evt.applicationId().equals(appId);
            assert evt.email().equals("user@mail.com");
            assert evt.newStatusId().equals(newState);
            assert "Approved".equals(evt.newStatusName());
        }

        @Test
        @DisplayName("Aprueba: si no hay nombre de estado, usa 'Updated'")
        void approveWithDefaultStatusNameUpdated() {
            int newState = Decision.APPROVED.toStateId();
            LoanApplication updated = LoanApplication.builder()
                    .applicationId(appId)
                    .email("user@mail.com")
                    .statusId(newState)
                    .build();

            when(repository.changeStatus(eq(appId), eq(newState)))
                    .thenReturn(Mono.just(updated));

            // No hay nombre de estado → defaultIfEmpty("Updated")
            when(stateRepository.findNameById(eq(newState))).thenReturn(Mono.empty());

            when(notifier.publishStatusChange(any())).thenReturn(Mono.empty());

            StepVerifier.create(useCase.execute(appId, Decision.APPROVED, advisor()))
                    .assertNext(res -> {
                        assert res.applicationId().equals(appId);
                        assert res.statusId().equals(newState);
                        assert "Updated".equals(res.statusName());
                    })
                    .verifyComplete();

            verify(repository).changeStatus(appId, newState);
            verify(stateRepository).findNameById(newState);
            verify(notifier).publishStatusChange(eventCaptor.capture());

            NotificationGateway.StatusChangedEvent evt = eventCaptor.getValue();
            assert "Updated".equals(evt.newStatusName());
        }

        @Test
        @DisplayName("Error en notifier: se propaga")
        void notifierErrorPropagates() {
            int newState = Decision.REJECTED.toStateId();
            LoanApplication updated = LoanApplication.builder()
                    .applicationId(appId)
                    .email("user@mail.com")
                    .statusId(newState)
                    .build();

            when(repository.changeStatus(eq(appId), eq(newState)))
                    .thenReturn(Mono.just(updated));

            when(stateRepository.findNameById(eq(newState)))
                    .thenReturn(Mono.just(State.builder().stateId(newState).name("Rejected").build()));

            when(notifier.publishStatusChange(any()))
                    .thenReturn(Mono.error(new RuntimeException("sns down")));

            StepVerifier.create(useCase.execute(appId, Decision.REJECTED, admin()))
                    .expectErrorMatches(ex -> ex instanceof RuntimeException && ex.getMessage().contains("sns down"))
                    .verify();

            verify(repository).changeStatus(appId, newState);
            verify(stateRepository).findNameById(newState);
            verify(notifier).publishStatusChange(any());
        }
    }
}
