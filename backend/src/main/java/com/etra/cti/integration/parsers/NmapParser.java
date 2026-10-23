package com.etra.cti.integration.parsers;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
public class NmapParser {

    private static final java.util.regex.Pattern ENTITY_PATTERN = java.util.regex.Pattern.compile("(?i)<!ENTITY\\b");
    private static final java.util.regex.Pattern BENIGN_NMAP_DOCTYPE = java.util.regex.Pattern.compile(
        "(?i)<!DOCTYPE\\s+nmaprun\\s*(?:SYSTEM\\s*[\"'][^\"']*nmap\\.dtd[\"'])?\\s*>"
    );

    public record NmapHost(String ip, int port, String protocol, String service) {}

    public List<NmapHost> parse(InputStream xml) throws Exception {
        byte[] rawBytes = xml.readAllBytes();
        String content = new String(rawBytes, java.nio.charset.StandardCharsets.UTF_8);

        if (ENTITY_PATTERN.matcher(content).find()) {
            throw new org.xml.sax.SAXParseException("Entity declarations are prohibited in XML uploads", null);
        }

        String sanitized = BENIGN_NMAP_DOCTYPE.matcher(content).replaceAll("");

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        try {
            dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
            dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
            dbf.setFeature(javax.xml.XMLConstants.FEATURE_SECURE_PROCESSING, true);
            dbf.setXIncludeAware(false);
            dbf.setExpandEntityReferences(false);
            try {
                dbf.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_DTD, "");
                dbf.setAttribute(javax.xml.XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            } catch (IllegalArgumentException ignored) {
                // Not supported on older or alternate parsers, disallow-doctype-decl is primary defense
            }
        } catch (javax.xml.parsers.ParserConfigurationException e) {
            throw new javax.xml.parsers.ParserConfigurationException("Critical XML security feature could not be set: " + e.getMessage());
        }
        DocumentBuilder builder = dbf.newDocumentBuilder();
        Document doc = builder.parse(new java.io.ByteArrayInputStream(sanitized.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        doc.getDocumentElement().normalize();

        List<NmapHost> out = new ArrayList<>();
        NodeList hosts = doc.getElementsByTagName("host");

        for (int i = 0; i < hosts.getLength(); i++) {
            Element host = (Element) hosts.item(i);

            String ip = null;
            NodeList addresses = host.getElementsByTagName("address");
            for (int a = 0; a < addresses.getLength(); a++) {
                Element addr = (Element) addresses.item(a);
                if ("ipv4".equals(addr.getAttribute("addrtype"))) {
                    ip = addr.getAttribute("addr");
                    break;
                }
            }

            NodeList ports = host.getElementsByTagName("port");
            for (int p = 0; p < ports.getLength(); p++) {
                Element port = (Element) ports.item(p);
                NodeList stateEls = port.getElementsByTagName("state");
                if (stateEls.getLength() == 0) continue;
                Element state = (Element) stateEls.item(0);
                if (!"open".equals(state.getAttribute("state"))) continue;

                int portId = Integer.parseInt(port.getAttribute("portid"));
                String protocol = port.getAttribute("protocol");

                String service = "";
                NodeList svcEls = port.getElementsByTagName("service");
                if (svcEls.getLength() > 0) {
                    service = ((Element) svcEls.item(0)).getAttribute("name");
                }

                out.add(new NmapHost(ip, portId, protocol, service));
            }
        }
        return out;
    }
}
