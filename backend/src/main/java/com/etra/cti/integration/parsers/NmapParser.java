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

    public record NmapHost(String ip, int port, String protocol, String service) {}

    public List<NmapHost> parse(InputStream xml) throws Exception {
        DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document doc = builder.parse(xml);
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
