package manage.model;

import java.io.Serializable;
import java.util.Map;

public interface PathUpdates extends Serializable {

    Map<String, Object> getPathUpdates();
}
