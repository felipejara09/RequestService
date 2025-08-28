package co.com.bancolombia.r2dbc.entity;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("application")
public class LoanApplicationEntity {
    @Id
    @Column("id_application")
    private String id;
    private BigDecimal amount;
    private Integer termMonths;
    private String email;
    private String identificationNumber;
    private Integer idState;
    private Integer idLoanType;

}
