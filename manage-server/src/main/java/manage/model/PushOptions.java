package manage.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class PushOptions {

    private boolean includeEB;
    private boolean includeOIDC;
    private boolean includePdP;
}
