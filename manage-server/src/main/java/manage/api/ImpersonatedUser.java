package manage.api;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@Getter
@AllArgsConstructor
@ToString
public class ImpersonatedUser {

    private String idpEntityId;
    private  String unspecifiedNameId;
    private  String displayName;

}
