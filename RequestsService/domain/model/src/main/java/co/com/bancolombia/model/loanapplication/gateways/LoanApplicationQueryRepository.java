package co.com.bancolombia.model.loanapplication.gateways;

import co.com.bancolombia.model.loanapplication.loanapplicaitonlist.PageResponse;
import co.com.bancolombia.model.loanapplication.loanapplicaitonlist.LoanApplicationSummary;
import reactor.core.publisher.Mono;

public interface LoanApplicationQueryRepository {
    Mono<PageResponse<LoanApplicationSummary>> listManualReview(int page, int size, String f);
}
