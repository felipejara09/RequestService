package co.com.bancolombia.usecase.registerloanapplication;


import co.com.bancolombia.model.auth.Actor;
import co.com.bancolombia.model.auth.Role;
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

    @InjectMocks RegisterLoanApplicationUseCase useCase;

    LoanApplication baseCmd;

    @BeforeEach
    void setUp() {
        baseCmd = LoanApplication.builder()
                .applicationId(null)
                .amount(new BigDecimal("150000"))
                .termMonths(12)
                .identificationNumber("102412357")
                .email("client@crediya.com")
                .statusId(null)
                .loanTypeId(1)
                .build();
    }

    //Devuelve UNAUTHORIZED cuando no hay actor (usuario autenticado).
    @Test
    void unauthorizedWhenNoActor() {
        StepVerifier.create(useCase.execute(baseCmd, null))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(DomainException.class);
                    assertThat(ex.getMessage()).isEqualTo("UNAUTHORIZED");
                })
                .verify();
        verifyNoInteractions(loanTypeRepository, customerGateway, loanApplicationRepository, stateRepository);
    }
    //Devuelve UNAUTHORIZED cuando el actor existe pero viene sin rol.
    @Test
    void unauthorizedWhenActorHasNullRole() {
        Actor actor = Actor.builder().role(null).email("x@x.com").build();
        StepVerifier.create(useCase.execute(baseCmd, actor))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(DomainException.class);
                    assertThat(ex.getMessage()).isEqualTo("UNAUTHORIZED");
                })
                .verify();
    }
    //Un CLIENTE no puede crear solicitud para otro correo; devuelve FORBIDDEN_OTHER_CUSTOMER.
    @Test
    void clientCannotCreateForAnotherEmail() {
        Actor client = Actor.builder().role(Role.CLIENT).email("other@crediya.com").build();

        StepVerifier.create(useCase.execute(baseCmd, client))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(DomainException.class);
                    assertThat(ex.getMessage()).isEqualTo("FORBIDDEN_OTHER_CUSTOMER");
                })
                .verify();
        verifyNoInteractions(loanTypeRepository, customerGateway, loanApplicationRepository, stateRepository);
    }

    // CLIENTE con email en token, pero el comando viene sin email -> debe prohibir
    @Test
    void clientEmailNull_forbiddenOtherCustomer() {

        Actor client = Actor.builder().role(Role.CLIENT).email("client@crediya.com").build();
        LoanApplication cmd = baseCmd.toBuilder().email(null).build();

        StepVerifier.create(useCase.execute(cmd, client))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(DomainException.class);
                    assertThat(ex.getMessage()).isEqualTo("FORBIDDEN_OTHER_CUSTOMER");
                })
                .verify();

        verifyNoInteractions(loanTypeRepository, customerGateway, loanApplicationRepository, stateRepository);
    }

    //Si no existe el tipo de préstamo, devuelve LOAN_TYPE_NOT_FOUND.
    @Test
    void loanTypeNotFound() {
        Actor advisor = Actor.builder().role(Role.ADVISOR).email("advisor@crediya.com").build();
        when(loanTypeRepository.findById(1)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(baseCmd, advisor))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(DomainException.class);
                    assertThat(ex.getMessage()).isEqualTo("LOAN_TYPE_NOT_FOUND");
                })
                .verify();
    }
    //Monto fuera de rango por DEBAJO del mínimo; devuelve AMOUNT_OUT_OF_RANGE.
    @Test
    void amountOutOfRange() {
        Actor advisor = Actor.builder().role(Role.ADVISOR).email("advisor@crediya.com").build();
        LoanType lt = LoanType.builder()
                .loanTypeId(1L).name("X")
                .minAmount(new BigDecimal("200000"))
                .maxAmount(new BigDecimal("300000"))
                .interestRate(10.0)
                .autovalidation(null)
                .build();
        when(loanTypeRepository.findById(1)).thenReturn(Mono.just(lt));

        StepVerifier.create(useCase.execute(baseCmd, advisor))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(DomainException.class);
                    assertThat(ex.getMessage()).isEqualTo("AMOUNT_OUT_OF_RANGE");
                })
                .verify();
        verify(customerGateway, never()).verifyIdentity(anyString(), anyString());
    }

    //Monto fuera de rango por ENCIMA del máximo; devuelve AMOUNT_OUT_OF_RANGE.
    @Test
    void amountAboveMax_isOutOfRange() {
        Actor advisor = Actor.builder().role(Role.ADVISOR).email("advisor@crediya.com").build();

        LoanType lt = LoanType.builder()
                .loanTypeId(1L).name("X")
                .minAmount(new BigDecimal("100000"))
                .maxAmount(new BigDecimal("120000")) // max por debajo del cmd.amount=150000
                .interestRate(10.0)
                .autovalidation(null)
                .build();

        when(loanTypeRepository.findById(1)).thenReturn(Mono.just(lt));

        StepVerifier.create(useCase.execute(baseCmd, advisor))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(DomainException.class);
                    assertThat(ex.getMessage()).isEqualTo("AMOUNT_OUT_OF_RANGE");
                })
                .verify();
    }
    //La verificación de identidad (gateway) responde false; devuelve CUSTOMER_NOT_VERIFIED.
    @Test
    void customerNotVerified() {
        Actor advisor = Actor.builder().role(Role.ADVISOR).email("advisor@crediya.com").build();
        LoanType lt = LoanType.builder()
                .loanTypeId(1L).name("Y")
                .minAmount(new BigDecimal("100000"))
                .maxAmount(new BigDecimal("300000"))
                .interestRate(11.5)
                .autovalidation(null)
                .build();
        when(loanTypeRepository.findById(1)).thenReturn(Mono.just(lt));
        when(customerGateway.verifyIdentity("102412357", "client@crediya.com")).thenReturn(Mono.just(false));

        StepVerifier.create(useCase.execute(baseCmd, advisor))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(DomainException.class);
                    assertThat(ex.getMessage()).isEqualTo("CUSTOMER_NOT_VERIFIED");
                })
                .verify();
        verify(loanApplicationRepository, never()).save(any());
    }

    //Flujo feliz como CLIENTE: persiste la solicitud y retorna el estado con su nombre.
    @Test
    void successFlowPersistsAndReturnsStatusName() {
        Actor client = Actor.builder().role(Role.CLIENT).email("client@crediya.com").build();

        LoanType lt = LoanType.builder()
                .loanTypeId(1L).name("Mortgage")
                .minAmount(new BigDecimal("100000"))
                .maxAmount(new BigDecimal("300000"))
                .interestRate(11.5)
                .autovalidation(null)
                .build();

        when(loanTypeRepository.findById(1)).thenReturn(Mono.just(lt));
        when(customerGateway.verifyIdentity("102412357", "client@crediya.com")).thenReturn(Mono.just(true));


        ArgumentCaptor<LoanApplication> savedCaptor = ArgumentCaptor.forClass(LoanApplication.class);
        UUID appId = UUID.randomUUID();
        LoanApplication saved = LoanApplication.builder()
                .applicationId(appId)
                .amount(baseCmd.getAmount())
                .termMonths(baseCmd.getTermMonths())
                .identificationNumber(baseCmd.getIdentificationNumber())
                .email(baseCmd.getEmail())
                .loanTypeId(baseCmd.getLoanTypeId())
                .statusId(1)
                .build();

        when(loanApplicationRepository.save(savedCaptor.capture())).thenReturn(Mono.just(saved));
        when(stateRepository.findNameById(1)).thenReturn(Mono.just(
                State.builder().stateId(1).name("Pending Review").description("x").build()
        ));

        StepVerifier.create(useCase.execute(baseCmd, client))
                .assertNext(res -> {
                    assertThat(res).isNotNull();
                    assertThat(res.applicationId()).isEqualTo(appId);
                    assertThat(res.statusId()).isEqualTo(1);
                    assertThat(res.statusName()).isEqualTo("Pending Review");
                })
                .verifyComplete();

        LoanApplication toPersist = savedCaptor.getValue();
        assertThat(toPersist.getEmail()).isEqualTo("client@crediya.com");
        assertThat(toPersist.getStatusId()).isEqualTo(1);
    }



    //Flujo feliz como ASESOR/ADMIN: persiste y retorna datos correctamente.
    @Test
    void successFlowAsAdvisor() {
        Actor advisor = Actor.builder().role(Role.ADVISOR).email("advisor@crediya.com").build();

        LoanType lt = LoanType.builder()
                .loanTypeId(1L).name("Mortgage")
                .minAmount(new BigDecimal("100000"))
                .maxAmount(new BigDecimal("300000"))
                .interestRate(11.5)
                .autovalidation(null)
                .build();

        when(loanTypeRepository.findById(1)).thenReturn(Mono.just(lt));
        when(customerGateway.verifyIdentity("102412357", "client@crediya.com")).thenReturn(Mono.just(true));

        UUID appId = UUID.randomUUID();
        LoanApplication saved = LoanApplication.builder()
                .applicationId(appId)
                .amount(baseCmd.getAmount())
                .termMonths(baseCmd.getTermMonths())
                .identificationNumber(baseCmd.getIdentificationNumber())
                .email(baseCmd.getEmail())
                .loanTypeId(baseCmd.getLoanTypeId())
                .statusId(1)
                .build();

        when(loanApplicationRepository.save(any())).thenReturn(Mono.just(saved));
        when(stateRepository.findNameById(1)).thenReturn(Mono.just(
                State.builder().stateId(1).name("Pending Review").description("x").build()
        ));

        StepVerifier.create(useCase.execute(baseCmd, advisor))
                .assertNext(res -> {
                    assertThat(res.applicationId()).isEqualTo(appId);
                    assertThat(res.statusId()).isEqualTo(1);
                    assertThat(res.statusName()).isEqualTo("Pending Review");
                })
                .verifyComplete();
    }

    //Si no se encuentra el nombre de estado en la BD, usa "Pending review" por defecto
    @Test
    void successFlow_usesDefaultStatusNameWhenStateMissing() {
        Actor advisor = Actor.builder().role(Role.ADVISOR).email("advisor@crediya.com").build();

        LoanType lt = LoanType.builder()
                .loanTypeId(1L).name("Mortgage")
                .minAmount(new BigDecimal("100000"))
                .maxAmount(new BigDecimal("300000"))
                .interestRate(11.5)
                .autovalidation(null)
                .build();

        when(loanTypeRepository.findById(1)).thenReturn(Mono.just(lt));
        when(customerGateway.verifyIdentity("102412357", "client@crediya.com")).thenReturn(Mono.just(true));

        UUID appId = UUID.randomUUID();
        LoanApplication saved = LoanApplication.builder()
                .applicationId(appId)
                .amount(baseCmd.getAmount())
                .termMonths(baseCmd.getTermMonths())
                .identificationNumber(baseCmd.getIdentificationNumber())
                .email(baseCmd.getEmail())
                .loanTypeId(baseCmd.getLoanTypeId())
                .statusId(1)
                .build();

        when(loanApplicationRepository.save(any())).thenReturn(Mono.just(saved));
        when(stateRepository.findNameById(1)).thenReturn(Mono.empty());

        StepVerifier.create(useCase.execute(baseCmd, advisor))
                .assertNext(res -> {
                    assertThat(res.statusName()).isEqualTo("Pending review");
                })
                .verifyComplete();
    }

    @Test
    void verifyIdentityErrorIsPropagated() {
        Actor advisor = Actor.builder().role(Role.ADVISOR).email("advisor@crediya.com").build();

        LoanType lt = LoanType.builder()
                .loanTypeId(1L).name("X")
                .minAmount(new BigDecimal("100000"))
                .maxAmount(new BigDecimal("300000"))
                .interestRate(11.5)
                .build();

        when(loanTypeRepository.findById(1)).thenReturn(Mono.just(lt));
        when(customerGateway.verifyIdentity("102412357", "client@crediya.com"))
                .thenReturn(Mono.error(new RuntimeException("boom")));

        StepVerifier.create(useCase.execute(baseCmd, advisor))
                .expectErrorMatches(e -> e instanceof RuntimeException && e.getMessage().equals("boom"))
                .verify();

        verify(loanApplicationRepository, never()).save(any());
    }

    @Test
    void repositorySaveErrorIsPropagated() {
        Actor advisor = Actor.builder().role(Role.ADVISOR).email("advisor@crediya.com").build();

        LoanType lt = LoanType.builder()
                .loanTypeId(1L).name("X")
                .minAmount(new BigDecimal("100000"))
                .maxAmount(new BigDecimal("300000"))
                .interestRate(11.5)
                .build();

        when(loanTypeRepository.findById(1)).thenReturn(Mono.just(lt));
        when(customerGateway.verifyIdentity("102412357", "client@crediya.com")).thenReturn(Mono.just(true));
        when(loanApplicationRepository.save(any())).thenReturn(Mono.error(new RuntimeException("db-error")));

        StepVerifier.create(useCase.execute(baseCmd, advisor))
                .expectErrorMatches(e -> e instanceof RuntimeException && e.getMessage().equals("db-error"))
                .verify();
    }

}
