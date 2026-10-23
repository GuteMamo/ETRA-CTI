package com.etra.cti.integration.parsers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.xml.sax.SAXParseException;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class ParserSecurityTest {

    @Test
    @DisplayName("BurpParser rejects XML containing DOCTYPE entity injection (XXE)")
    void testBurpParserRejectsXxe() {
        BurpParser parser = new BurpParser();
        String xxePayload = """
            <?xml version="1.0"?>
            <!DOCTYPE issues [
              <!ENTITY xxe SYSTEM "file:///etc/passwd">
            ]>
            <issues>
              <issue>
                <name>&xxe;</name>
                <severity>High</severity>
              </issue>
            </issues>
            """;

        ByteArrayInputStream stream = new ByteArrayInputStream(xxePayload.getBytes(StandardCharsets.UTF_8));
        assertThrows(SAXParseException.class, () -> parser.parse(stream));
    }

    @Test
    @DisplayName("NmapParser rejects XML containing DOCTYPE entity injection (XXE)")
    void testNmapParserRejectsXxe() {
        NmapParser parser = new NmapParser();
        String xxePayload = """
            <?xml version="1.0"?>
            <!DOCTYPE nmaprun [
              <!ENTITY xxe SYSTEM "file:///etc/passwd">
            ]>
            <nmaprun>
              <host>
                <address addr="127.0.0.1"/>
                <ports>
                  <port protocol="tcp" portid="80">
                    <state state="open"/>
                    <service name="http" product="Apache"/>
                  </port>
                </ports>
              </host>
            </nmaprun>
            """;

        ByteArrayInputStream stream = new ByteArrayInputStream(xxePayload.getBytes(StandardCharsets.UTF_8));
        assertThrows(SAXParseException.class, () -> parser.parse(stream));
    }

    @Test
    @DisplayName("BurpParser parses valid benign XML without error")
    void testBurpParserBenignXml() throws Exception {
        BurpParser parser = new BurpParser();
        String benignPayload = """
            <?xml version="1.0" encoding="utf-8"?>
            <issues>
              <issue>
                <name>Cross-site scripting (reflected)</name>
                <severity>High</severity>
                <host>https://example.com</host>
                <path>/search</path>
                <issueDetail>Reflected XSS in search query parameter</issueDetail>
              </issue>
            </issues>
            """;

        ByteArrayInputStream stream = new ByteArrayInputStream(benignPayload.getBytes(StandardCharsets.UTF_8));
        var findings = parser.parse(stream);
        assertNotNull(findings);
        assertEquals(1, findings.size());
        assertEquals("Cross-site scripting (reflected)", findings.get(0).title());
    }

    @Test
    @DisplayName("NmapParser parses valid benign XML without error")
    void testNmapParserBenignXml() throws Exception {
        NmapParser parser = new NmapParser();
        String benignPayload = """
            <?xml version="1.0" encoding="utf-8"?>
            <nmaprun>
              <host>
                <address addr="10.0.0.5" addrtype="ipv4"/>
                <ports>
                  <port protocol="tcp" portid="443">
                    <state state="open"/>
                    <service name="https" product="nginx"/>
                  </port>
                </ports>
              </host>
            </nmaprun>
            """;

        ByteArrayInputStream stream = new ByteArrayInputStream(benignPayload.getBytes(StandardCharsets.UTF_8));
        var hosts = parser.parse(stream);
        assertNotNull(hosts);
        assertEquals(1, hosts.size());
        assertEquals("10.0.0.5", hosts.get(0).ip());
        assertEquals(443, hosts.get(0).port());
        assertEquals("https", hosts.get(0).service());
    }

    @Test
    @DisplayName("BurpParser and NmapParser reject malformed XML with SAXParseException")
    void testParsersRejectMalformedXml() {
        BurpParser burpParser = new BurpParser();
        NmapParser nmapParser = new NmapParser();
        String malformed = "<issues><issue><name>unclosed tag</issues>";

        assertThrows(SAXParseException.class, () ->
            burpParser.parse(new ByteArrayInputStream(malformed.getBytes(StandardCharsets.UTF_8))));
        assertThrows(SAXParseException.class, () ->
            nmapParser.parse(new ByteArrayInputStream(malformed.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    @DisplayName("BurpParser and NmapParser reject external network entity injection without network access")
    void testParsersRejectExternalNetworkEntities() {
        BurpParser burpParser = new BurpParser();
        NmapParser nmapParser = new NmapParser();
        String networkEntityPayload = """
            <?xml version="1.0"?>
            <!DOCTYPE issues [
              <!ENTITY ext SYSTEM "http://127.0.0.1:65534/nonexistent.dtd">
            ]>
            <issues>
              <issue>
                <name>&ext;</name>
              </issue>
            </issues>
            """;

        assertThrows(SAXParseException.class, () ->
            burpParser.parse(new ByteArrayInputStream(networkEntityPayload.getBytes(StandardCharsets.UTF_8))));
        assertThrows(SAXParseException.class, () ->
            nmapParser.parse(new ByteArrayInputStream(networkEntityPayload.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    @DisplayName("BurpParser and NmapParser reject recursive entity expansion (Billion Laughs)")
    void testParsersRejectEntityExpansion() {
        BurpParser burpParser = new BurpParser();
        NmapParser nmapParser = new NmapParser();
        String billionLaughsPayload = """
            <?xml version="1.0"?>
            <!DOCTYPE lolz [
              <!ENTITY lol "lol">
              <!ENTITY lol2 "&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;&lol;">
              <!ENTITY lol3 "&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;&lol2;">
            ]>
            <issues>
              <issue>
                <name>&lol3;</name>
              </issue>
            </issues>
            """;

        assertThrows(SAXParseException.class, () ->
            burpParser.parse(new ByteArrayInputStream(billionLaughsPayload.getBytes(StandardCharsets.UTF_8))));
        assertThrows(SAXParseException.class, () ->
            nmapParser.parse(new ByteArrayInputStream(billionLaughsPayload.getBytes(StandardCharsets.UTF_8))));
    }

    @Test
    @DisplayName("BurpParser parses real Burp Suite XML export with burp-issues.dtd DOCTYPE")
    void testBurpParserWithRealBurpDoctype() throws Exception {
        BurpParser parser = new BurpParser();
        String realBurpXml = """
            <?xml version="1.0" encoding="utf-8"?>
            <!DOCTYPE issues SYSTEM "burp-issues.dtd">
            <issues burpVersion="2024.10.1" exportTime="Sat Oct 11 15:30:00 EAT 2026">
              <issue>
                <serialNumber>1234567890123456789</serialNumber>
                <type>1049088</type>
                <name>SQL injection</name>
                <host ip="127.0.0.1">http://localhost:8080</host>
                <path><![CDATA[/dvwa/vulnerabilities/sqli/]]></path>
                <location><![CDATA[URL parameter 'id']]></location>
                <severity>High</severity>
                <issueDetail><![CDATA[SQL injection in id]]></issueDetail>
              </issue>
            </issues>
            """;

        ByteArrayInputStream stream = new ByteArrayInputStream(realBurpXml.getBytes(StandardCharsets.UTF_8));
        var findings = parser.parse(stream);
        assertNotNull(findings);
        assertEquals(1, findings.size());
        assertEquals("SQL injection", findings.get(0).title());
    }

    @Test
    @DisplayName("NmapParser parses real Nmap XML export with nmap.dtd DOCTYPE")
    void testNmapParserWithRealNmapDoctype() throws Exception {
        NmapParser parser = new NmapParser();
        String realNmapXml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE nmaprun SYSTEM "https://svn.nmap.org/nmap/docs/nmap.dtd">
            <nmaprun scanner="nmap" args="nmap -sV 10.0.0.5" start="1690000000" version="7.94">
              <host>
                <address addr="10.0.0.5" addrtype="ipv4"/>
                <ports>
                  <port protocol="tcp" portid="443">
                    <state state="open"/>
                    <service name="https" product="nginx"/>
                  </port>
                </ports>
              </host>
            </nmaprun>
            """;

        ByteArrayInputStream stream = new ByteArrayInputStream(realNmapXml.getBytes(StandardCharsets.UTF_8));
        var hosts = parser.parse(stream);
        assertNotNull(hosts);
        assertEquals(1, hosts.size());
        assertEquals("10.0.0.5", hosts.get(0).ip());
    }
}
