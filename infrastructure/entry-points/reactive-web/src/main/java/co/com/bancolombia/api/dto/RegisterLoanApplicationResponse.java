package co.com.bancolombia.api.dto;

import java.util.UUID;

public record RegisterLoanApplicationResponse(UUID applicationId,
                                              Integer statudId,
                                              String statusName) {}
