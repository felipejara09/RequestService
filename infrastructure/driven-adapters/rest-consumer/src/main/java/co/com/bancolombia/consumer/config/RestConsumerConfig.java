package co.com.bancolombia.consumer.config;

import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import static io.netty.channel.ChannelOption.CONNECT_TIMEOUT_MILLIS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Configuration
public class RestConsumerConfig {

    @Bean
    public WebClient customerWebClient(WebClient.Builder builder,
                                       @Value("${customer.service.base-url}") String baseUrl) {
        return builder
                .baseUrl(baseUrl)
                .filter(propagateAuthFromContext())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .exchangeStrategies(ExchangeStrategies.builder()
                        .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                        .build())
                .build();
    }

    private ExchangeFilterFunction propagateAuthFromContext() {
        return (request, next) -> Mono.deferContextual(ctx -> {
            if (ctx.hasKey("AUTH_TOKEN")) {
                String auth = ctx.get("AUTH_TOKEN");
                ClientRequest newReq = ClientRequest.from(request)
                        .headers(h -> h.set(HttpHeaders.AUTHORIZATION, auth))
                        .build();
                return next.exchange(newReq);
            }
            return next.exchange(request);
        });
    }
}
