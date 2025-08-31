package co.com.bancolombia.consumer;

import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class ObjectRequest {

    private String identificationNumber;
    private String email;
}
