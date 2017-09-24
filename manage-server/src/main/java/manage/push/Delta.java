package manage.push;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;

@Getter
@AllArgsConstructor
@EqualsAndHashCode(of = {"entityId", "attribute"})
public class Delta {

    private String entityId;
    private String attribute;
    private Object prePushValue;
    private Object postPushValue;

}
