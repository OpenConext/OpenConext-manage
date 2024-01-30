package manage.api;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ImpersonatedUser {

    private String idpEntityId;
    private  String unspecifiedNameId;
    private  String displayName;

}
