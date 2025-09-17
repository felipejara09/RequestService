package co.com.bancolombia.api.dto;


import co.com.bancolombia.model.loanapplication.LoanApplication;

import java.math.BigDecimal;


public record RegisterLoanApplicationRequest(
        String identificationNumber,
        String email,
        BigDecimal amount,
        Integer termMonths,
        Integer loanTypeId) {

    public LoanApplication toDomain() {
        return LoanApplication.builder()
                .applicationId(null)
                .identificationNumber(identificationNumber)
                .email(email)
                .amount(amount)
                .termMonths(termMonths)
                .loanTypeId(loanTypeId)
                .statusId(null)
                .build();
    }
}