package co.com.bancolombia.usecase;


import java.util.UUID;


public record RegisterLoanApplicationResult(
        UUID applicationId,
        Integer statusId,
        String statusName
) {
}
