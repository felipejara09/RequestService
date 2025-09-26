package co.com.bancolombia.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record CapacityRequestDto(
        UUID applicationId,
        String identificationNumber,
        String email,
        BigDecimal amount,
        Integer termMonths,
        Double annualInterestRate,
        BigDecimal monthlyIncome,
        BigDecimal currentMonthlyDebt
) {}
