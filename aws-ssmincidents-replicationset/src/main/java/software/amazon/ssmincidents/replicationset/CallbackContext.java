package software.amazon.ssmincidents.replicationset;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import software.amazon.cloudformation.proxy.StdCallbackContext;

@Getter
@Setter
@ToString
@EqualsAndHashCode(callSuper = false)
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
public class CallbackContext extends StdCallbackContext {
    @JsonProperty("retryAttemptsRemaining")
    private Integer awaitRetryAttemptsRemaining;

    @JsonProperty("mainAPICalled")
    private Boolean mainAPICalled;

    public boolean mainAPICalled() {
        return (mainAPICalled != null) && mainAPICalled;
    }
}
