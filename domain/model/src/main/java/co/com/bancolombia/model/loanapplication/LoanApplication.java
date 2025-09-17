package co.com.bancolombia.model.loanapplication;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;


@Getter
@Setter
@AllArgsConstructor
@Value
@Builder(toBuilder = true)
public class LoanApplication {

    UUID applicationId;
    BigDecimal amount;
    Integer termMonths;
    String identificationNumber;
    String email;
    Integer statusId;
    Integer loanTypeId;
}
