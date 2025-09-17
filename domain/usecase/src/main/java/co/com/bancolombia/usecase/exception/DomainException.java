package co.com.bancolombia.usecase.exception;

public class DomainException extends RuntimeException {
    public DomainException(String code) {
        super(code);
    }
}
