package co.com.bancolombia.usecase.listmanualreview;

import co.com.bancolombia.model.auth.Actor;
import co.com.bancolombia.model.auth.Role;
import co.com.bancolombia.model.loanapplication.gateways.LoanApplicationQueryRepository;
import co.com.bancolombia.model.loanapplication.loanapplicaitonlist.LoanApplicationSummary;
import co.com.bancolombia.model.loanapplication.loanapplicaitonlist.PageResponse;
import co.com.bancolombia.usecase.exception.DomainException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ListManualReviewUseCaseTest {

    @Mock LoanApplicationQueryRepository queryRepository;
    @InjectMocks ListManualReviewUseCase useCase;


    //Devuelve UNAUTHORIZED cuando no hay actor (usuario autenticado).
    @Test
    void unauthorizedWhenNoActor() {
        StepVerifier.create(useCase.execute(0, 20, null, null))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(DomainException.class);
                    assertThat(ex.getMessage()).isEqualTo("UNAUTHORIZED");
                })
                .verify();
        verifyNoInteractions(queryRepository);
    }

    //Un CLIENTE no puede listar solicitudes de revisión; devuelve FORBIDDEN.
    @Test
    void forbiddenForClientRole() {
        Actor client = Actor.builder().role(Role.CLIENT).email("c@c.com").build();

        StepVerifier.create(useCase.execute(0, 20, null, client))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(DomainException.class);
                    assertThat(ex.getMessage()).isEqualTo("FORBIDDEN");
                })
                .verify();
        verifyNoInteractions(queryRepository);
    }

    //Normaliza page<0 a 0, limita size>100 a 100 y convierte q a trim+lowercase antes de llamar al repo.
    @Test
    void normalizesPageSizeAndFilter() {
        Actor advisor = Actor.builder().role(Role.ADVISOR).email("a@c.com").build();

        PageResponse<LoanApplicationSummary> page =
                new PageResponse<>(List.of(), 0, 10, 0, 0);


        when(queryRepository.listManualReview(anyInt(), anyInt(), any()))
                .thenReturn(Mono.just(page));

        StepVerifier.create(useCase.execute(-5, 200, "  TeSt@Mail.COM  ", advisor))
                .expectNext(page)
                .verifyComplete();

        ArgumentCaptor<Integer> pageCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<Integer> sizeCap = ArgumentCaptor.forClass(Integer.class);
        ArgumentCaptor<String> termCap = ArgumentCaptor.forClass(String.class);

        verify(queryRepository).listManualReview(pageCap.capture(), sizeCap.capture(), termCap.capture());
        assertThat(pageCap.getValue()).isEqualTo(0);       // -5 -> 0
        assertThat(sizeCap.getValue()).isEqualTo(100);     // 200 -> 100 (máximo)
        assertThat(termCap.getValue()).isEqualTo("test@mail.com"); // trimmed + lowercase
    }


    //Cuando f viene en blanco/espacios, lo envía como null al repositorio.
    @Test
    void nullFilterWhenBlank() {
        Actor admin = Actor.builder().role(Role.ADMIN).email("admin@c.com").build();
        PageResponse<LoanApplicationSummary> page =
                new PageResponse<>(List.of(), 0, 20, 0, 0);

        when(queryRepository.listManualReview(anyInt(), anyInt(), isNull()))
                .thenReturn(Mono.just(page));

        StepVerifier.create(useCase.execute(0, 20, "   ", admin))
                .expectNext(page)
                .verifyComplete();

        verify(queryRepository).listManualReview(eq(0), eq(20), isNull());
    }

    //Si size=0 (o menor al mínimo), lo ajusta al mínimo permitido (1) antes de llamar al repo.
    @Test
    void sizeLowerBoundIsClampedToOne() {
        Actor advisor = Actor.builder().role(Role.ADVISOR).email("a@c.com").build();
        PageResponse<LoanApplicationSummary> page =
                new PageResponse<>(List.of(), 0, 1, 0, 0);

        when(queryRepository.listManualReview(eq(0), eq(1), isNull()))
                .thenReturn(Mono.just(page));

        StepVerifier.create(useCase.execute(0, 0, null, advisor))
                .expectNext(page)
                .verifyComplete();

        verify(queryRepository).listManualReview(eq(0), eq(1), isNull());
    }

    @Test
    void unauthorizedWhenActorHasNullRole() {
        Actor actor = Actor.builder().role(null).email("x@x.com").build();

        StepVerifier.create(useCase.execute(0, 20, null, actor))
                .expectErrorSatisfies(ex -> {
                    assertThat(ex).isInstanceOf(DomainException.class);
                    assertThat(ex.getMessage()).isEqualTo("UNAUTHORIZED");
                })
                .verify();

        verifyNoInteractions(queryRepository);
    }


}