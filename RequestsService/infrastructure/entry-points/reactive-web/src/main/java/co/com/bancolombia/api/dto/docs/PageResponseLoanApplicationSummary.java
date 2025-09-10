package co.com.bancolombia.api.dto.docs;

import co.com.bancolombia.model.loanapplication.loanapplicaitonlist.LoanApplicationSummary;
import co.com.bancolombia.model.loanapplication.loanapplicaitonlist.PageResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "PageResponseLoanApplicationSummary",
        description = "Respuesta paginada de solicitudes para revisión manual")
public class PageResponseLoanApplicationSummary {
    public List<LoanApplicationSummary> content;
    public int page;
    public int size;
    public long totalElements;
    public int totalPages;
}
