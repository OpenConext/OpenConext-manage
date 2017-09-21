package manage.migration;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ArpDeserializerTest {

    private ArpDeserializer subject = new ArpDeserializer();

    @Test
    public void parseArpAttributesComplex() throws Exception {
        String input = "a:7:{s:38:\"urn:mace:dir:attribute-def:displayName\";a:1:{i:0;s:1:\"*\";}s:29:\"urn:mace:dir:attribute-def:cn\";a:1:{i:0;s:1:\"*\";}s:36:\"urn:mace:dir:attribute-def:givenName\";a:1:{i:0;s:1:\"*\";}s:29:\"urn:mace:dir:attribute-def:sn\";a:1:{i:0;s:1:\"*\";}s:47:\"urn:mace:dir:attribute-def:eduPersonAffiliation\";a:3:{i:0;s:5:\"test*\";i:1;s:1:\"*\";i:2;s:5:\"exact\";}s:47:\"urn:mace:dir:attribute-def:eduPersonEntitlement\";a:3:{i:0;s:5:\"test1\";i:1;s:5:\"test2\";i:2;s:5:\"test3\";}s:41:\"urn:mace:dir:attribute-def:eduPersonOrcid\";a:1:{i:0;s:1:\"*\";}}";
        Map<String, Object> arp = subject.parseArpAttributes(input);
        assertTrue(isEnabled(arp));

        assertEquals(7, Map.class.cast(arp.get("attributes")).size());

        List<Map<String, String>> displayNames = this.arpValue(arp, "urn:mace:dir:attribute-def:displayName");
        assertEquals(1, displayNames.size());

        Map<String, String> value = displayNames.get(0);
        assertEquals("*", value.get("value"));
        assertEquals("idp", value.get("source"));

        List<Map<String, String>> affiliations = this.arpValue(arp, "urn:mace:dir:attribute-def:eduPersonAffiliation");
        assertEquals(3, affiliations.size());

        assertEquals(Arrays.asList("*", "exact", "test*"),
            affiliations.stream().map(affiliation -> affiliation.get("value")).sorted().collect(toList()));
    }

    @Test
    public void parseArpAttributesNoArp() throws Exception {
        String input = "N;";
        Map<String, Object> arp = subject.parseArpAttributes(input);
        assertFalse(isEnabled(arp));
        assertTrue(Map.class.cast(arp.get("attributes")).isEmpty());
    }

    @Test
    public void parseArpAttributesEmptyArp() throws Exception {
        String input = "a:0:{}";
        Map<String, Object> arp = subject.parseArpAttributes(input);
        assertTrue(isEnabled(arp));
        assertTrue(Map.class.cast(arp.get("attributes")).isEmpty());
    }


    @Test
    public void parseArpNewStyle() throws Exception {
        String input = "a:7:{s:46:\"urn:mace:dir:attribute-def:eduPersonTargetedID\";a:1:{i:0;a:1:{s:5:\"value\";s:1:\"*\";}}s:38:\"urn:mace:dir:attribute-def:displayName\";a:1:{i:0;a:1:{s:5:\"value\";s:1:\"*\";}}s:29:\"urn:mace:dir:attribute-def:cn\";a:1:{i:0;a:1:{s:5:\"value\";s:1:\"*\";}}s:29:\"urn:mace:dir:attribute-def:sn\";a:1:{i:0;a:1:{s:5:\"value\";s:1:\"*\";}}s:31:\"urn:mace:dir:attribute-def:mail\";a:1:{i:0;a:1:{s:5:\"value\";s:1:\"*\";}}s:47:\"urn:mace:dir:attribute-def:eduPersonAffiliation\";a:1:{i:0;a:1:{s:5:\"value\";s:1:\"*\";}}s:30:\"urn:mace:dir:attribute-def:uid\";a:1:{i:0;a:1:{s:5:\"value\";s:1:\"*\";}}}";
        Map<String, Object> arp = subject.parseArpAttributes(input);

        assertEquals(7, Map.class.cast(arp.get("attributes")).size());
    }

    @Test
    public void parseNewArpIncludingSource() throws Exception {
        String input = "a:3:{s:46:\"urn:mace:dir:attribute-def:eduPersonTargetedID\";a:1:{i:0;a:1:{s:5:\"value\";s:1:\"*\";}}s:47:\"urn:mace:dir:attribute-def:eduPersonAffiliation\";a:2:{i:0;a:2:{s:5:\"value\";s:0:\"\";s:6:\"source\";s:5:\"orcid\";}i:1;a:2:{s:5:\"value\";s:4:\"test\";s:6:\"source\";s:5:\"orcid\";}}s:53:\"urn:mace:dir:attribute-def:eduPersonScopedAffiliation\";a:1:{i:0;a:2:{s:5:\"value\";s:4:\"reg*\";s:6:\"source\";s:3:\"sab\";}}}";
        Map<String, Object> arp = subject.parseArpAttributes(input);

        List<Map<String, String>> affiliations = arpValue(arp, "urn:mace:dir:attribute-def:eduPersonAffiliation");

        assertEquals(2,affiliations.size());

        Map<String, String> affiliation = affiliations.get(0);
        assertEquals("*", affiliation.get("value"));
        assertEquals("orcid", affiliation.get("source"));

        affiliation = affiliations.get(1);
        assertEquals("test", affiliation.get("value"));
        assertEquals("orcid", affiliation.get("source"));

    }

    private boolean isEnabled(Map<String, Object> arp) {
        return Boolean.class.cast(arp.get("enabled"));
    }

    private List<Map<String, String>> arpValue(Map<String, Object> arp, String attributeName) {
        return (List<Map<String, String>>) Map.class.cast(arp.get("attributes")).get(attributeName);
    }
}