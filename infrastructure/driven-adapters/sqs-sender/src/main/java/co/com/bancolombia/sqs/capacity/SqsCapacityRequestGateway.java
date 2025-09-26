package co.com.bancolombia.sqs.capacity;

import co.com.bancolombia.model.debtcapacity.gateways.CapacityRequestGateway;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;

@Slf4j
@Component
@RequiredArgsConstructor
public class SqsCapacityRequestGateway implements CapacityRequestGateway {
    private final SqsAsyncClient sqs;
    private final ObjectMapper om = new ObjectMapper();
    @Value("${aws.app.sqs.capacity-queue-url}") private String queueUrl;

    @Override public Mono<Void> publish(CapacityRequestGateway.CapacityRequest event) {
        try {
            String body = om.writeValueAsString(event);
            var req = SendMessageRequest.builder().queueUrl(queueUrl).messageBody(body).build();
            return Mono.fromFuture(sqs.sendMessage(req))
                    .doOnSuccess(r -> log.info("Capacity request sent. msgId={}", r.messageId()))
                    .then();
        } catch (Exception e) { return Mono.error(e); }
    }
}
