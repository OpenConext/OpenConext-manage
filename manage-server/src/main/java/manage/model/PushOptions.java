package manage.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@NoArgsConstructor
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
