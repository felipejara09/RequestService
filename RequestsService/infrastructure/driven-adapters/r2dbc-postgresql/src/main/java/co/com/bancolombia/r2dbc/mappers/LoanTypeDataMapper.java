package co.com.bancolombia.r2dbc.mappers;

import co.com.bancolombia.model.loanapplication.LoanType;
import co.com.bancolombia.r2dbc.entity.LoanTypeEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LoanTypeDataMapper {
    LoanType toModel(LoanTypeEntity loanTypeEntity);
}
