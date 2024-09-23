package manage.policies;

import lombok.SneakyThrows;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.xmlunit.builder.DiffBuilder;
import org.xmlunit.builder.Input;
import org.xmlunit.diff.Diff;

import static org.junit.Assert.assertFalse;

public class PolicyDiffTest {

    @SneakyThrows
    @Test
    public void diffPolicies() {
        Diff d = DiffBuilder
                .compare(Input.fromStream(new ClassPathResource("/pdp/policy.xml").getInputStream()))
                .withTest(Input.fromStream(new ClassPathResource("/pdp/policy_copy.xml").getInputStream()))
                .ignoreWhitespace()
                .normalizeWhitespace()
                .ignoreElementContentWhitespace()
                .build();
        assertFalse(d.hasDifferences());
    }

}