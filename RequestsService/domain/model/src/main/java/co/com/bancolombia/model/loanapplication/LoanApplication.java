package co.com.bancolombia.model.loanapplication;
import lombok.*;

import java.math.BigDecimal;


@Getter
@Setter
@AllArgsConstructor
@Value
@Builder(toBuilder = true)
public class LoanApplication {

    String id;
    BigDecimal amount;
    Integer termMonths;
    String IdentificationNumber;
    String email;
    Integer statusId;
    Integer loanTypeId;
}
