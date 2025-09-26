package co.com.bancolombia.usecase.requestcapacity;

import co.com.bancolombia.model.auth.Actor;
import co.com.bancolombia.model.auth.Role;
import co.com.bancolombia.model.debtcapacity.gateways.CapacityRequestGateway;
import co.com.bancolombia.usecase.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class RequestCapacityCalculationUseCaseTest {

    private CapacityRequestGateway queue;
    private RequestCapacityCalculationUseCase useCase;

    @BeforeEach
    void setUp() {
        queue = mock(CapacityRequestGateway.class);
        useCase = new RequestCapacityCalculationUseCase(queue);
    }

    private RequestCapacityCalculationUseCase.Cmd validCmd() {
        return new RequestCapacityCalculationUseCase.Cmd(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "10203040",
                "customer@mail.com",
                new BigDecimal("10000000"),
                36,
                20.0,
                new BigDecimal("3000000"),
                new BigDecimal("500000")
        );
    }

    private Actor adminActor() {
        Role role = mock(Role.class);
        when(role.isAdminOrAdvisor()).thenReturn(true);

        Actor actor = mock(Actor.class);
        when(actor.getRole()).thenReturn(role);
        return actor;
    }

    private Actor viewerActor() {
        Role role = mock(Role.class);
        when(role.isAdminOrAdvisor()).thenReturn(false);

        Actor actor = mock(Actor.class);
        when(actor.getRole()).thenReturn(role);
        return actor;
    }

    @Test
    void executes_ok_publishes_to_gateway() {
        var cmd = validCmd();
        when(queue.publish(any())).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(cmd, adminActor()))
                .verifyComplete();

        verify(queue, times(1)).publish(argThat(matchesCmd(cmd)));
        verifyNoMoreInteractions(queue);
    }

    @Test
    void null_actor_unauthorized() {
        StepVerifier.create(useCase.execute(validCmd(), null))
                .expectErrorSatisfies(err ->
                        org.junit.jupiter.api.Assertions.assertTrue(
                                err instanceof DomainException && err.getMessage().contains("UNAUTHORIZED")))
                .verify();

        verifyNoInteractions(queue);
    }

    @Test
    void role_without_permission_forbidden() {
        StepVerifier.create(useCase.execute(validCmd(), viewerActor()))
                .expectErrorSatisfies(err ->
                        org.junit.jupiter.api.Assertions.assertTrue(
                                err instanceof DomainException && err.getMessage().contains("FORBIDDEN")))
                .verify();

        verifyNoInteractions(queue);
    }

    @Test
    void propagates_gateway_error() {
        var cmd = validCmd();
        when(queue.publish(any())).thenReturn(Mono.error(new RuntimeException("SQS down")));

        StepVerifier.create(useCase.execute(cmd, adminActor()))
                .expectErrorMatches(e -> e.getMessage().contains("SQS down"))
                .verify();
    }


    private static ArgumentMatcher<CapacityRequestGateway.CapacityRequest> matchesCmd(
            RequestCapacityCalculationUseCase.Cmd c) {
        return e ->
                e.applicationId().equals(c.applicationId()) &&
                        e.identificationNumber().equals(c.identificationNumber()) &&
                        e.email().equals(c.email()) &&
                        e.amount().compareTo(c.amount()) == 0 &&
                        e.termMonths().equals(c.termMonths()) &&
                        Double.compare(e.annualInterestRate(), c.annualInterestRate()) == 0 &&
                        e.monthlyIncome().compareTo(c.monthlyIncome()) == 0 &&
                        e.currentMonthlyDebt().compareTo(c.currentMonthlyDebt()) == 0;
    }
}
