package co.com.bancolombia.r2dbc.mappers;

import co.com.bancolombia.model.loanapplication.LoanApplication;
import co.com.bancolombia.r2dbc.entity.LoanApplicationEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LoanApplicationMapper {
    LoanApplicationEntity toData(LoanApplication loanApplication);
    LoanApplication toModel(LoanApplicationEntity loanApplicationEntity);
}
