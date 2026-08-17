package manage.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor(onConstructor_ = @JsonCreator)
@AllArgsConstructor
@Getter
@Setter
@ToString
public class PushOptions {

    private boolean includeEB;
    private boolean includeOIDC;
    private boolean includePdP;
    private boolean includeStepUp   ;
}
