package manage.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrphanMetaData {

    private String missingEntityId;
    private String referencedByEntityId;
    private String referencedByName;
    private String referencedCollectionName;
    private String id;
    private String collection;

}
