package co.com.bancolombia.model.loanapplication.loanapplicaitonlist;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

@Value
@Builder(toBuilder = true)
public class LoanApplicationSummary {
    UUID applicationId;
    BigDecimal amount;
    Integer termMonths;
    String email;
    String identificationNumber;
    String loanTypeName;
    Double interestRate;
    String statusName;
    BigDecimal monthlyPayment;
}
