package mr.migration;

import org.junit.Test;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
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
        ArpAttributes arpAttributes = subject.parseArpAttributes(input);
        assertTrue(arpAttributes.isEnabled());

        Map<String, List<ArpValue>> arp = arpAttributes.getAttributes();
        assertEquals(7, arp.size());

        List<ArpValue> displayNames = arp.get("urn:mace:dir:attribute-def:displayName");
        assertEquals(1, displayNames.size());

        ArpValue displayName = displayNames.get(0);
        assertEquals("*", displayName.getValue());
        assertEquals("idp", displayName.getSource());

        List<ArpValue> affiliations = arp.get("urn:mace:dir:attribute-def:eduPersonAffiliation");
        assertEquals(3, affiliations.size());

        assertEquals(Arrays.asList("*", "exact", "test*"), affiliations.stream().map(ArpValue::getValue).sorted().collect(toList()));
    }

    @Test
    public void parseArpAttributesNoArp() throws Exception {
        String input = "N;";
        ArpAttributes arpAttributes = subject.parseArpAttributes(input);
        assertFalse(arpAttributes.isEnabled());
        assertTrue(arpAttributes.getAttributes().isEmpty());
    }

    @Test
    public void parseArpAttributesEmptyArp() throws Exception {
        String input = "a:0:{}";
        ArpAttributes arpAttributes = subject.parseArpAttributes(input);
        assertTrue(arpAttributes.isEnabled());
        assertTrue(arpAttributes.getAttributes().isEmpty());
    }


    @Test
    public void parseArpNewStyle() throws Exception {
        String input = "a:7:{s:46:\"urn:mace:dir:attribute-def:eduPersonTargetedID\";a:1:{i:0;a:1:{s:5:\"value\";s:1:\"*\";}}s:38:\"urn:mace:dir:attribute-def:displayName\";a:1:{i:0;a:1:{s:5:\"value\";s:1:\"*\";}}s:29:\"urn:mace:dir:attribute-def:cn\";a:1:{i:0;a:1:{s:5:\"value\";s:1:\"*\";}}s:29:\"urn:mace:dir:attribute-def:sn\";a:1:{i:0;a:1:{s:5:\"value\";s:1:\"*\";}}s:31:\"urn:mace:dir:attribute-def:mail\";a:1:{i:0;a:1:{s:5:\"value\";s:1:\"*\";}}s:47:\"urn:mace:dir:attribute-def:eduPersonAffiliation\";a:1:{i:0;a:1:{s:5:\"value\";s:1:\"*\";}}s:30:\"urn:mace:dir:attribute-def:uid\";a:1:{i:0;a:1:{s:5:\"value\";s:1:\"*\";}}}";
        ArpAttributes arpAttributes = subject.parseArpAttributes(input);

        assertEquals(7, arpAttributes.getAttributes().size());
    }

    @Test
    public void parseNewArpIncludingSource() throws Exception {
        String input = "a:3:{s:46:\"urn:mace:dir:attribute-def:eduPersonTargetedID\";a:1:{i:0;a:1:{s:5:\"value\";s:1:\"*\";}}s:47:\"urn:mace:dir:attribute-def:eduPersonAffiliation\";a:2:{i:0;a:2:{s:5:\"value\";s:0:\"\";s:6:\"source\";s:5:\"orcid\";}i:1;a:2:{s:5:\"value\";s:4:\"test\";s:6:\"source\";s:5:\"orcid\";}}s:53:\"urn:mace:dir:attribute-def:eduPersonScopedAffiliation\";a:1:{i:0;a:2:{s:5:\"value\";s:4:\"reg*\";s:6:\"source\";s:3:\"sab\";}}}";
        ArpAttributes arpAttributes = subject.parseArpAttributes(input);

        List<ArpValue> arpValues = arpAttributes.getAttributes().get("urn:mace:dir:attribute-def:eduPersonAffiliation");
        assertEquals(2,arpValues.size());

        ArpValue arpValue = arpValues.get(0);
        assertEquals("*", arpValue.getValue());
        assertEquals("orcid", arpValue.getSource());

        arpValue = arpValues.get(1);
        assertEquals("test", arpValue.getValue());
        assertEquals("orcid", arpValue.getSource());
    }
}