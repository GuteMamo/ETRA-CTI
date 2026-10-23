package com.etra.cti.integration.parsers;

import com.etra.cti.detection.Severity;
import com.etra.cti.integration.ImportedFinding;
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
public class BurpParser {

    private static final java.util.regex.Pattern ENTITY_PATTERN = java.util.regex.Pattern.compile("(?i)<!ENTITY\\b");
    private static final java.util.regex.Pattern BENIGN_BURP_DOCTYPE = java.util.regex.Pattern.compile(
        "(?i)<!DOCTYPE\\s+issues\\s*(?:SYSTEM\\s*[\"'][^\"']*burp-issues\\.dtd[\"'])?\\s*>"
    );

    public List<ImportedFinding> parse(InputStream xml) throws Exception {
        byte[] rawBytes = xml.readAllBytes();
        String content = new String(rawBytes, java.nio.charset.StandardCharsets.UTF_8);

        if (ENTITY_PATTERN.matcher(content).find()) {
            throw new org.xml.sax.SAXParseException("Entity declarations are prohibited in XML uploads", null);
        }

        String sanitized = BENIGN_BURP_DOCTYPE.matcher(content).replaceAll("");

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

        List<ImportedFinding> out = new ArrayList<>();
        NodeList issues = doc.getElementsByTagName("issue");

        for (int i = 0; i < issues.getLength(); i++) {
            Element issue = (Element) issues.item(i);
            String name = text(issue, "name");
            String severity = text(issue, "severity");
            String host = text(issue, "host");
            String path = text(issue, "path");
            String location = text(issue, "location");
            String detail = text(issue, "issueDetail");
            String background = text(issue, "issueBackground");

            String endpoint = host != null && path != null ? host + path : path;

            out.add(new ImportedFinding(
                name == null ? "Burp Issue" : name,
                background != null ? background : detail,
                endpoint,
                location,
                mapSeverity(severity),
                detail,
                "BURP"
            ));
        }
        return out;
    }

    private String text(Element el, String tag) {
        NodeList nl = el.getElementsByTagName(tag);
        return nl.getLength() > 0 ? nl.item(0).getTextContent() : null;
    }

    private Severity mapSeverity(String s) {
        if (s == null) return Severity.MEDIUM;
        return switch (s.toLowerCase()) {
            case "high" -> Severity.HIGH;
            case "medium" -> Severity.MEDIUM;
            case "low" -> Severity.LOW;
            case "information", "info" -> Severity.LOW;
            default -> Severity.MEDIUM;
        };
    }
}
