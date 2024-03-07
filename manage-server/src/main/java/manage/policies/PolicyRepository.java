package manage.policies;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class PolicyRepository {

    private final JdbcTemplate jdbcTemplate;
    private final RowMapper<Map<String, String>> rowMapper = (rs, rowNum) -> {
        Map<String, String> policy = new HashMap<>();
        policy.put("name", rs.getString(1));
        policy.put("xml", rs.getString(2));
        return policy;
    };

    @Autowired
    public PolicyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, String>> policies() {
        return jdbcTemplate.query("SELECT name, policy_xml FROM pdp_policies WHERE latest_revision = 1", this.rowMapper);
    }

    public List<Map<String, String>> migratedPolicies() {
        return jdbcTemplate.query("SELECT name, policy_xml FROM pdp_migrated_policies", this.rowMapper);
    }
}
