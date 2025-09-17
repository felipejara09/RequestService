package co.com.bancolombia.model.loanapplication;
import lombok.*;


@Getter
@Setter
@AllArgsConstructor
@Value
@Builder(toBuilder = true)
public class State {
    private Integer stateId;
    private String name;
    private String description;
}
