package co.com.bancolombia.r2dbc.adapters;

import co.com.bancolombia.model.loanapplication.gateways.LoanApplicationQueryRepository;
import co.com.bancolombia.model.loanapplication.loanapplicaitonlist.LoanApplicationSummary;
import co.com.bancolombia.model.loanapplication.loanapplicaitonlist.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class LoanApplicationQueryRepositoryAdapter implements LoanApplicationQueryRepository {

    private final DatabaseClient db;

    @Override
    public Mono<PageResponse<LoanApplicationSummary>> listManualReview(int page, int size, String f) {
        int offset = page * size;

        String baseWhere = """
            WHERE la.id_state IN (1, 3,4)   
        """;

        String search = (f != null)
                ? " AND (LOWER(la.email) LIKE :like OR la.identification_number LIKE :idLike) "
                : "";


        String selectSql = """
            SELECT
              la.id_application         AS application_id,
              la.amount                 AS amount,
              la.term_months            AS term_months,
              la.email                  AS email,
              la.identification_number  AS identification_number,
              lt.name                   AS loan_type_name,
              lt.interest_rate          AS interest_rate,
              s.name                    AS status_name,
              ROUND(
                CASE
                  WHEN lt.interest_rate IS NULL OR lt.interest_rate = 0 OR la.term_months = 0
                    THEN la.amount / NULLIF(la.term_months, 0)
                  ELSE
                    la.amount * ((lt.interest_rate/100.0)/12.0) / (1 - POWER(1 + (lt.interest_rate/100.0)/12.0, -la.term_months))
                    END
              , 2) AS monthly_payment
            FROM application la
            JOIN loan_type lt ON lt.id_loan_type = la.id_loan_type
            JOIN states s     ON s.id_state      = la.id_state
            %s
            ORDER BY la.created_at DESC
            LIMIT :limit OFFSET :offset
        """.formatted(baseWhere + search);

        var sel = db.sql(selectSql)
                .bind("limit", size)
                .bind("offset", offset);

        if (f != null) {
            sel = sel.bind("like", "%" + f + "%")
                    .bind("idLike", "%" + f + "%");
        }

        var itemsMono = sel.map((row, meta) -> LoanApplicationSummary.builder()
                        .applicationId(row.get("application_id", UUID.class))
                        .amount(row.get("amount", BigDecimal.class))
                        .termMonths(row.get("term_months", Integer.class))
                        .email(row.get("email", String.class))
                        .identificationNumber(row.get("identification_number", String.class))
                        .loanTypeName(row.get("loan_type_name", String.class))
                        .interestRate(row.get("interest_rate", Double.class))
                        .statusName(row.get("status_name", String.class))
                        .monthlyPayment(row.get("monthly_payment", BigDecimal.class))
                        .build())
                .all()
                .collectList();

        String countSql = """
            SELECT COUNT(*) AS c
            FROM application la
            JOIN loan_type lt ON lt.id_loan_type = la.id_loan_type
            JOIN states s     ON s.id_state      = la.id_state
            %s
        """.formatted(baseWhere + search);

        var cnt = db.sql(countSql);
        if (f != null) {
            cnt = cnt.bind("like", "%" + f + "%")
                    .bind("idLike", "%" + f + "%");
        }

        var countMono = cnt.map((row, meta) -> row.get("c", Long.class))
                .one().defaultIfEmpty(0L);

        return Mono.zip(itemsMono, countMono)
                .map(t -> {
                    List<LoanApplicationSummary> content = t.getT1();
                    long total = t.getT2();
                    int totalPages = (int) Math.ceil(total / (double) size);
                    return new PageResponse<>(content, page, size, total, totalPages);
                });
    }
}
