package manage.policies;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class PolicyRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper rowMapper = (rs, rowNum) ->
            Map.of("name", rs.getString(1), "description", rs.getString(2), "xml", rs.getString(3));

    @Autowired
    public PolicyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, String>> policies() {
        return jdbcTemplate.query("SELECT name, description, policy_xml FROM pdp_policies WHERE latest_revision = 1", this.rowMapper);
    }

    public List<Map<String, String>> migratedPolicies() {
        return jdbcTemplate.query("SELECT name, description, policy_xml FROM pdp_migrated_policies", this.rowMapper);
    }
}
