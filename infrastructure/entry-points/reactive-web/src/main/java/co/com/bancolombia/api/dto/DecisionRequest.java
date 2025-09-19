package co.com.bancolombia.api.dto;

import java.util.UUID;

public record DecisionRequest(
        UUID applicationId,
        String decision
) {}

