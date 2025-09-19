package co.com.bancolombia.api.dto;

import java.util.UUID;

public record DecisionResponse(
        UUID applicationId,
        Integer statusId,
        String statusName
) {}
