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

    public List<ImportedFinding> parse(InputStream xml) throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(xml);
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
