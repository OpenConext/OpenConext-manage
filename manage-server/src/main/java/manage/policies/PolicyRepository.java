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
    private final RowMapper<PolicySummary> rowMapper = (rs, rowNum) -> new PolicySummary(
            rs.getString(1),
            rs.getString(2),
            rs.getBoolean(3)
    );

    @Autowired
    public PolicyRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<PolicySummary> policies() {
        return jdbcTemplate.query("SELECT name, policy_xml, is_active FROM pdp_policies WHERE latest_revision = 1", this.rowMapper);
    }

    public List<PolicySummary> migratedPolicies() {
        return jdbcTemplate.query("SELECT name, policy_xml, is_active FROM pdp_migrated_policies", this.rowMapper);
    }
}
