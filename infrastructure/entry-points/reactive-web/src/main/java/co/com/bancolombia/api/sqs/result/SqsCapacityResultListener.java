package co.com.bancolombia.api.sqs.result;

import co.com.bancolombia.model.debtcapacity.gateways.CapacityResult;
import co.com.bancolombia.model.notification.gateways.NotificationGateway;
import co.com.bancolombia.usecase.applycapacity.ApplyCapacityDecisionUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.time.Duration;

@RequiredArgsConstructor
@Component
@Slf4j
public class SqsCapacityResultListener {

    private final SqsAsyncClient sqs;
    private final ApplyCapacityDecisionUseCase applyUC;
    private final NotificationGateway notifier;
    private final ObjectMapper om;

    @Value("${aws.app.sqs.capacity-result-queue-url}")
    private String resultQueueUrl;

    @PostConstruct
    public void start() {
        Flux.interval(Duration.ofSeconds(1))
                .flatMap(t -> receiveBatch().onErrorResume(e -> {
                    log.error("SQS receive error", e);
                    return Mono.empty();
                }))
                .subscribe();
    }

    private Mono<Void> receiveBatch() {
        var req = ReceiveMessageRequest.builder()
                .queueUrl(resultQueueUrl)
                .waitTimeSeconds(10)
                .maxNumberOfMessages(5)
                .build();

        return Mono.fromFuture(sqs.receiveMessage(req))
                .flatMapMany(resp -> Flux.fromIterable(resp.messages()))
                .flatMap(this::   processOne, 1)
                .then();
    }

    private boolean isApproved(CapacityResult res) {
        if (res == null || res.finalStatus() == null) return false;
        String s = String.valueOf(res.finalStatus()).trim();


        if (s.chars().allMatch(Character::isDigit)) {
            return "2".equals(s);
        }


        String u = s.toUpperCase();
        return u.equals("APPROVED")
                || u.equals("APROBADO")
                || u.equals("APROBADA")
                || u.equals("APROVADO")
                || u.equals("APROVADA");
    }


    private Mono<Void> processOne(Message m) {
        final String body = m.body();
        try {
            CapacityResult res = om.readValue(body, CapacityResult.class);
            log.info("Capacity finalStatus={}, appId={}", res.finalStatus(), res.applicationId());

            var cmd = new ApplyCapacityDecisionUseCase.Cmd(res.applicationId(), res.finalStatus());

            return applyUC.executeSilent(cmd)
                    .doOnSuccess(v -> log.info("Capacity result applied. appId={}", res.applicationId()))
                    .then(Mono.defer(() -> {
                        if (!isApproved(res)) return Mono.empty();
                        var evt = new NotificationGateway.StatusChangedEvent(
                                res.applicationId(),
                                2,
                                res.monthlyPayment()
                        );
                        return notifier.publishStatusChange(evt)
                                .doOnSuccess(v -> log.info("StatusChanged published from capacity flow. appId={}", res.applicationId()));
                    }))
                    .then(delete(m.receiptHandle()))
                    .onErrorResume(e -> {
                        log.error("Apply result failed. body={}", body, e);
                        return delete(m.receiptHandle());
                    });

        } catch (Exception ex) {
            log.error("Invalid message, deleting. body={}", body, ex);
            return delete(m.receiptHandle());
        }
    }

    private Mono<Void> delete(String receiptHandle) {
        var del = DeleteMessageRequest.builder()
                .queueUrl(resultQueueUrl)
                .receiptHandle(receiptHandle)
                .build();
        return Mono.fromFuture(sqs.deleteMessage(del)).then();
    }
}
