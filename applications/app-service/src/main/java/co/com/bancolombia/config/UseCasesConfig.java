package co.com.bancolombia.config;

import co.com.bancolombia.model.debtcapacity.gateways.CapacityRequestGateway;
import co.com.bancolombia.model.loanapplication.gateways.LoanApplicationRepository;
import co.com.bancolombia.model.loanapplication.gateways.StateRepository;
import co.com.bancolombia.model.notification.gateways.NotificationGateway;
import co.com.bancolombia.usecase.applycapacity.ApplyCapacityDecisionUseCase;
import co.com.bancolombia.usecase.decideloan.DecideLoanApplicationUseCase;
import co.com.bancolombia.usecase.requestcapacity.RequestCapacityCalculationUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(basePackages = "co.com.bancolombia.usecase",
        includeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "^.+UseCase$")
        },
        useDefaultFilters = false)
public class UseCasesConfig {
        @Bean
        public DecideLoanApplicationUseCase decideLoanApplicationUseCase(
                LoanApplicationRepository statusRepo,
                StateRepository stateRepository,
                NotificationGateway notifier
        ) {
                return new DecideLoanApplicationUseCase(statusRepo, stateRepository, notifier);
        }
    @Bean
    public RequestCapacityCalculationUseCase requestCapacityCalculationUseCase(
            CapacityRequestGateway capacityQueue
    ) { return new RequestCapacityCalculationUseCase(capacityQueue); }

    @Bean
    public ApplyCapacityDecisionUseCase applyCapacityDecisionUseCase(
            LoanApplicationRepository repo, StateRepository stateRepo, NotificationGateway notifier
    ) { return new ApplyCapacityDecisionUseCase(repo, stateRepo, notifier); }
}
