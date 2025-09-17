package co.com.bancolombia.model.loanapplication.loanapplicaitonlist;

import java.util.List;

public record PageResponse<T>(
        List<T> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}

