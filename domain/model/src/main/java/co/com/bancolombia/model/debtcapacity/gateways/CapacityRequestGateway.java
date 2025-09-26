package co.com.bancolombia.model.debtcapacity.gateways;

import reactor.core.publisher.Mono;
import java.math.BigDecimal;
import java.util.UUID;

public interface CapacityRequestGateway {
    Mono<Void> publish(CapacityRequest event);

    record CapacityRequest(
            UUID applicationId,
            String identificationNumber,
            String email,
            BigDecimal amount,
            Integer termMonths,
            Double annualInterestRate,
            BigDecimal monthlyIncome,
            BigDecimal currentMonthlyDebt
    ) {}
}
