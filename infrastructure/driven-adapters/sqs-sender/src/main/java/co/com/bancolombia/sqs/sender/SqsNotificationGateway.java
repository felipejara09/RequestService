package co.com.bancolombia.sqs.sender;

import co.com.bancolombia.model.notification.gateways.NotificationGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Slf4j
@Component
@Profile({"default","prod"})
@RequiredArgsConstructor
public class SqsNotificationGateway implements NotificationGateway {

    private final SqsAsyncClient sqs;
    private final ObjectMapper om;

    @Value("${aws.app.sqs.status-queue-url}")
    private String queueUrl;

    @Override
    public Mono<Void> publishStatusChange(StatusChangedEvent evt) {

        return Mono.fromCallable(() -> om.writeValueAsString(evt))
                .flatMap(body -> {
                    var req = SendMessageRequest.builder()
                            .queueUrl(queueUrl)
                            .messageBody(body)
                            .build();
                    return Mono.fromFuture(sqs.sendMessage(req));
                })
                .doOnSuccess(r -> log.info("SQS sent: messageId={}", r.messageId()))
                .then();
    }
}
