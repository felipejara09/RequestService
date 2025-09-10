package co.com.bancolombia.usecase.listmanualreview;

import co.com.bancolombia.model.auth.Actor;
import co.com.bancolombia.model.loanapplication.gateways.LoanApplicationQueryRepository;
import co.com.bancolombia.model.loanapplication.loanapplicaitonlist.LoanApplicationSummary;
import co.com.bancolombia.model.loanapplication.loanapplicaitonlist.PageResponse;
import co.com.bancolombia.usecase.exception.DomainException;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class ListManualReviewUseCase {

    private final LoanApplicationQueryRepository queryRepository;

    public Mono<PageResponse<LoanApplicationSummary>> execute(int page, int size, String f, Actor actor) {
        if (actor == null || actor.getRole() == null) {
            return Mono.error(new DomainException("UNAUTHORIZED"));
        }
        if (!actor.getRole().isAdminOrAdvisor()) {
            return Mono.error(new DomainException("FORBIDDEN"));
        }
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        String term = (f == null || f.isBlank()) ? null : f.trim().toLowerCase();
        return queryRepository.listManualReview(safePage, safeSize, term);
    }
}
