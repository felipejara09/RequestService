package co.com.bancolombia.r2dbc.entity;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("application")
public class LoanApplicationEntity {

    @Id
    @Column("id_application")
    private UUID applicationId;
    private BigDecimal amount;
    @Column("term_months")
    private Integer termMonths;
    private String identificationNumber;
    private String email;
    @Column("id_state")
    private Integer statusId;
    @Column("id_loan_type")
    private Integer loanTypeId;
}
