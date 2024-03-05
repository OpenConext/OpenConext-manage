package manage.policies;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class PolicyRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public PolicyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, String>> policies() {
        return jdbcTemplate.query("SELECT name, policy_xml FROM pdp_policies WHERE latest_revision = 1", (rs, rowNum) ->
                Map.of("name", rs.getString(1), "xml", rs.getString(2))
        );
    }

    public List<Map<String, String>> migratedPolicies() {
        return jdbcTemplate.query("SELECT name, policy_xml FROM pdp_migrated_policies", (rs, rowNum) ->
                Map.of("name", rs.getString(1), "xml", rs.getString(2))
        );
    }
}
