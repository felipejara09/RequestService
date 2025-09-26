package co.com.bancolombia.model.debtcapacity.gateways;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record CapacityResult(
        UUID applicationId,
        String finalStatus,
        String email,
        BigDecimal monthlyPayment,
        List<Installment> schedule
) {
    public record Installment(int number, BigDecimal interest, BigDecimal principal, BigDecimal remaining) {}
}