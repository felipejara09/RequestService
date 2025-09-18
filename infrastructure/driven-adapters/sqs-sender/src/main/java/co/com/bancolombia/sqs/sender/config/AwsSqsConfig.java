package co.com.bancolombia.sqs.sender.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.http.nio.netty.NettyNioAsyncHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

@Configuration
public class AwsSqsConfig {
    @Bean
    public SqsAsyncClient sqsClient(@Value("${aws.region:us-east-1}") String region) {
        return SqsAsyncClient.builder()
                .region(Region.of(region))
                .httpClientBuilder(NettyNioAsyncHttpClient.builder())
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}