package mr.control;

import mr.model.MetaData;
import mr.repository.MetaDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetaDataController {

    @Autowired
    private MetaDataRepository metaDataRepository;

    @GetMapping("/client/metadata/{type}/{id}")
    public MetaData get(@PathVariable("type") String type, @PathVariable("id") String id) {
        MetaData byId = metaDataRepository.findById(id, type);
        return byId;
    }


}
