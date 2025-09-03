package co.com.bancolombia.usecase.registerloanapplication;


import co.com.bancolombia.model.loanapplication.*;
import co.com.bancolombia.model.loanapplication.gateways.*;
import co.com.bancolombia.usecase.exception.DomainException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RegisterLoanApplicationUseCaseTest{


    @Mock CustumerServiceGateway customerGateway;
    @Mock LoanTypeRepository loanTypeRepository;
    @Mock LoanApplicationRepository loanApplicationRepository;
    @Mock StateRepository stateRepository;

    @InjectMocks
    RegisterLoanApplicationUseCase useCase;

    private LoanApplication cmd;
    private LoanType loanTypeOk;

    private static final UUID APP_ID =
            UUID.fromString("80b28def-481b-425f-9515-7a4ee4dec127");

    @BeforeEach
    void setUp() {
        cmd = LoanApplication.builder()
                .identificationNumber("123456789")
                .email("user@test.com")
                .amount(new BigDecimal("5000"))
                .termMonths(24)
                .loanTypeId(10)
                .build();

        loanTypeOk = LoanType.builder()
                .loanTypeId(1L)
                .minAmount(new BigDecimal("1000"))
                .maxAmount(new BigDecimal("10000"))
                .build();
    }

    @Test
    void shouldErrorWhenLoanTypeNotFound() {
        when(loanTypeRepository.findById(10)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(cmd))
                .expectErrorSatisfies(err -> {
                    assertThat(err).isInstanceOf(DomainException.class);
                    assertThat(err.getMessage()).isEqualTo("LOAN_TYPE_NOT_FOUND");
                })
                .verify();

        verify(customerGateway, never()).verifyIdentity(anyString(), anyString());
        verifyNoInteractions(loanApplicationRepository, stateRepository);
    }

    @Test
    void shouldErrorWhenAmountOutOfRange() {

        LoanApplication badCmd = cmd.toBuilder().amount(new BigDecimal("500")).build();

        when(loanTypeRepository.findById(10)).thenReturn(Mono.just(loanTypeOk));

        StepVerifier.create(useCase.execute(badCmd))
                .expectErrorSatisfies(err -> {
                    assertThat(err).isInstanceOf(DomainException.class);
                    assertThat(err.getMessage()).isEqualTo("AMOUNT_OUT_OF_RANGE");
                })
                .verify();

        verify(customerGateway, never()).verifyIdentity(anyString(), anyString());
        verifyNoInteractions(loanApplicationRepository, stateRepository);
    }

    @Test
    void shouldErrorWhenCustomerNotVerified() {
        when(loanTypeRepository.findById(10)).thenReturn(Mono.just(loanTypeOk));
        when(customerGateway.verifyIdentity("123456789", "user@test.com")).thenReturn(Mono.just(false));

        StepVerifier.create(useCase.execute(cmd))
                .expectErrorSatisfies(err -> {
                    assertThat(err).isInstanceOf(DomainException.class);
                    assertThat(err.getMessage()).isEqualTo("CUSTOMER_NOT_VERIFIED");
                })
                .verify();

        verify(loanApplicationRepository, never()).save(any());
        verify(stateRepository, never()).findNameById(any());
    }

    @Test
    void shouldPersistAndReturnResultWithStateName() {
        when(loanTypeRepository.findById(10)).thenReturn(Mono.just(loanTypeOk));
        when(customerGateway.verifyIdentity("123456789", "user@test.com")).thenReturn(Mono.just(true));

        LoanApplication saved = cmd.toBuilder()
                .applicationId(APP_ID)
                .statusId(1)
                .build();
        when(loanApplicationRepository.save(any(LoanApplication.class))).thenReturn(Mono.just(saved));

        when(stateRepository.findNameById(1)).thenReturn(Mono.just(State.builder().name("En validación").build()));

        StepVerifier.create(useCase.execute(cmd))
                .assertNext(result -> {
                    assertThat(result.applicationId()).isEqualTo(APP_ID);
                    assertThat(result.statusId()).isEqualTo(1);
                    assertThat(result.statusName()).isEqualTo("En validación");
                })
                .verifyComplete();

        ArgumentCaptor<LoanApplication> captor = ArgumentCaptor.forClass(LoanApplication.class);
        verify(loanApplicationRepository).save(captor.capture());
        LoanApplication toSave = captor.getValue();
        assertThat(toSave.getIdentificationNumber()).isEqualTo("123456789");
        assertThat(toSave.getEmail()).isEqualTo("user@test.com");
        assertThat(toSave.getAmount()).isEqualByComparingTo("5000");
        assertThat(toSave.getTermMonths()).isEqualTo(24);
        assertThat(toSave.getLoanTypeId()).isEqualTo(10);
        assertThat(toSave.getStatusId()).isEqualTo(1);
    }

    @Test
    void shouldPersistAndReturnDefaultStatusWhenStateEmpty() {
        when(loanTypeRepository.findById(10)).thenReturn(Mono.just(loanTypeOk));
        when(customerGateway.verifyIdentity("123456789", "user@test.com")).thenReturn(Mono.just(true));

        LoanApplication saved = cmd.toBuilder()
                .applicationId(APP_ID)
                .statusId(1)
                .build();
        when(loanApplicationRepository.save(any(LoanApplication.class))).thenReturn(Mono.just(saved));

        when(stateRepository.findNameById(1)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(cmd))
                .assertNext(result -> {
                    assertThat(result.applicationId()).isEqualTo(APP_ID);
                    assertThat(result.statusId()).isEqualTo(1);
                    assertThat(result.statusName()).isEqualTo("Pending review");
                })
                .verifyComplete();
    }
}
