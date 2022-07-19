package manage.model;

import java.io.Serializable;
import java.util.Map;

public interface PathUpdates extends Serializable {

    String getMetaDataId();

    String getType();

    Map<String, Object> getExternalReferenceData();

    Map<String, Object> getPathUpdates();

    boolean isIncrementalChange();

    PathUpdateType getPathUpdateType();
}
