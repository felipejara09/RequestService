package co.com.bancolombia.usecase.listmanualreview;

import co.com.bancolombia.model.loanapplication.gateways.LoanApplicationQueryRepository;
import co.com.bancolombia.model.loanapplication.loanapplicaitonlist.LoanApplicationSummary;
import co.com.bancolombia.model.loanapplication.loanapplicaitonlist.PageResponse;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class ListManualReviewUseCase {

    private final LoanApplicationQueryRepository queryRepository;

    public Mono<PageResponse<LoanApplicationSummary>> execute(int page, int size, String q) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        String term = (q == null || q.isBlank()) ? null : q.trim().toLowerCase();
        return queryRepository.listManualReview(safePage, safeSize, term);
    }
}
