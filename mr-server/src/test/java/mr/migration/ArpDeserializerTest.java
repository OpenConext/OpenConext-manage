package mr.migration;

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

}