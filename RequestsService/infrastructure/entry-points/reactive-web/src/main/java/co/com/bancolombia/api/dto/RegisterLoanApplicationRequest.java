package co.com.bancolombia.api.dto;


import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RegisterLoanApplicationRequest {
    @NotNull
    @DecimalMin(value = "0.0", inclusive = false)
    BigDecimal amount;
    @NotNull @Min(1) Integer termMonths;
    @NotBlank @Email String email;
    @NotBlank String identificationNumber; // NEW
    @NotNull Integer loanTypeId;
}
