package co.com.bancolombia.model.auth;

import lombok.Builder;
import lombok.Value;

@Value
@Builder(toBuilder = true)
public class Actor {
    Integer userId;
    Role role;
    String email;
}
