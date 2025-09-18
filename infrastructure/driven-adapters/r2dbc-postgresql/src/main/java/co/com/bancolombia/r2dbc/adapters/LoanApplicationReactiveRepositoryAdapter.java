package co.com.bancolombia.r2dbc.adapters;

import co.com.bancolombia.model.loanapplication.LoanApplication;
import co.com.bancolombia.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.r2dbc.entity.LoanApplicationEntity;
import co.com.bancolombia.r2dbc.gateways.LoanApplicationReactiveRepository;
import co.com.bancolombia.r2dbc.mappers.LoanApplicationMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.UUID;


@RequiredArgsConstructor
@Repository
public class LoanApplicationReactiveRepositoryAdapter implements LoanApplicationRepository {

    private final LoanApplicationReactiveRepository repository;
    private final LoanApplicationMapper mapper;
    private final DatabaseClient db;

    @Override
    public Mono<LoanApplication> save(LoanApplication application) {
        LoanApplicationEntity data = mapper.toData(application);
        return repository.save(data).map(mapper::toModel);
    }

    @Override
    public Mono<LoanApplication> changeStatus(UUID applicationId, Integer newStatusId) {

        return db.sql("UPDATE application SET id_state = :st WHERE id_application = :id")
                .bind("st", newStatusId)
                .bind("id", applicationId)
                .fetch().rowsUpdated()
                .flatMap(rows -> rows == 0
                        ? Mono.error(new Exception("error"))
                        : Mono.empty())

                .then(db.sql("""
                        SELECT id_application, amount, term_months, identification_number, email, id_state, id_loan_type
                        FROM application WHERE id_application = :id
                        """)
                        .bind("id", applicationId)
                        .map((row, meta) -> LoanApplication.builder()
                                .applicationId(row.get("id_application", UUID.class))
                                .amount(row.get("amount", BigDecimal.class))
                                .termMonths(row.get("term_months", Integer.class))
                                .identificationNumber(row.get("identification_number", String.class))
                                .email(row.get("email", String.class))
                                .statusId(row.get("id_state", Integer.class))
                                .loanTypeId(row.get("id_loan_type", Integer.class))
                                .build())
                        .one());
    }
}


