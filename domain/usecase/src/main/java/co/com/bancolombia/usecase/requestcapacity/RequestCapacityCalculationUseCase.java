package co.com.bancolombia.usecase.requestcapacity;

import co.com.bancolombia.model.auth.Actor;
import co.com.bancolombia.model.debtcapacity.gateways.CapacityRequestGateway;
import co.com.bancolombia.usecase.exception.DomainException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;

@RequiredArgsConstructor
public class RequestCapacityCalculationUseCase {
    private final CapacityRequestGateway queue;

    public record Cmd(
            UUID applicationId, String identificationNumber, String email,
            BigDecimal amount, Integer termMonths, Double annualInterestRate,
            BigDecimal monthlyIncome, BigDecimal currentMonthlyDebt
    ) {}

    public Mono<Void> execute(Cmd cmd, Actor actor) {
        if (actor == null || actor.getRole() == null) return Mono.error(new DomainException("UNAUTHORIZED"));
        if (!actor.getRole().isAdminOrAdvisor())      return Mono.error(new DomainException("FORBIDDEN"));
        var evt = new CapacityRequestGateway.CapacityRequest(
                cmd.applicationId(), cmd.identificationNumber(), cmd.email(),
                cmd.amount(), cmd.termMonths(), cmd.annualInterestRate(),
                cmd.monthlyIncome(), cmd.currentMonthlyDebt());
        return queue.publish(evt);
    }
}
