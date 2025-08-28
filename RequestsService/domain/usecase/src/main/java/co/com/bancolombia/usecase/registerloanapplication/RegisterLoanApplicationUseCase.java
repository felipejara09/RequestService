package co.com.bancolombia.usecase.registerloanapplication;


import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class RegisterLoanApplicationUseCase {
    return loanTypeRepository.findById(input.getLoanTypeId())
            .

    switchIfEmpty(Mono.error(new IllegalArgumentException("Loan type not found")))
            .

    flatMap(lt ->

    {
        var amount = input.getAmount();
        if (amount.compareTo(lt.getMinAmount()) < 0 || amount.compareTo(lt.getMaxAmount()) > 0) {
            return Mono.error(new IllegalArgumentException("Amount out of allowed range"));
        }
        var enriched = input.toBuilder()
                .id(UUID.randomUUID().toString())   // ensure ID in domain
                .statusId(PENDING_REVIEW_STATUS_ID)
                .build();
        return applicationRepository.save(enriched);
    });

}
