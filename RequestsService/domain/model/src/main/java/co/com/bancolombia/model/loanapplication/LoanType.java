package co.com.bancolombia.model.loanapplication;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@Value
@Builder(toBuilder = true)
public class LoanType {

    Long loanTypeId;
    String name;
    BigDecimal minAmount;
    BigDecimal maxAmount;
    Double interestRate;
    Boolean autovalidation;

}
