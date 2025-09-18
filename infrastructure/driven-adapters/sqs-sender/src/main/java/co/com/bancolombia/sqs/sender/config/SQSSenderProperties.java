package co.com.bancolombia.sqs.sender.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aws")
public class SQSSenderProperties {
    private String region;
    private String endpointOverride; // ej: http://localhost:4566 (vacío en AWS real)

    @Data
    public static class Sqs {
        private String queueUrl;
    }
    private Sqs sqs = new Sqs();
}
