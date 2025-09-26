package co.com.bancolombia.model.notification.gateways;

import reactor.core.publisher.Mono;


import java.math.BigDecimal;
import java.util.UUID;

public interface NotificationGateway {
    Mono<Void> publishStatusChange(StatusChangedEvent event);

    record StatusChangedEvent(UUID applicationId, Integer newStatusId, BigDecimal amount) {}
}
