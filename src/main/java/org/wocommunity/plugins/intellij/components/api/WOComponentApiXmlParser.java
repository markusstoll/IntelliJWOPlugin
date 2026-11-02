package org.wocommunity.plugins.intellij.components.api;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses and serializes WebObjects Component API XML definitions (.api).
 */
public class WOComponentApiXmlParser {

    /**
     * Parses the given XML content into a WOComponentApiModel.
     */
    public static WOComponentApiModel parse(String xmlText) {
        WOComponentApiModel model = new WOComponentApiModel();
        if (xmlText == null || xmlText.isBlank()) {
            return model;
        }

        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(false);
            try {
                dbf.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                dbf.setFeature("http://xml.org/sax/features/external-general-entities", false);
                dbf.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            } catch (Exception ignored) {}

            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new InputSource(new StringReader(xmlText)));
            Element root = doc.getDocumentElement();
            if (root == null || !"wodefinitions".equals(root.getTagName())) {
                return model;
            }

            NodeList woList = root.getElementsByTagName("wo");
            if (woList.getLength() == 0) {
                return model;
            }

            Element woElem = (Element) woList.item(0);
            model.setComponentClassName(woElem.getAttribute("class"));
            String contentAttr = woElem.getAttribute("wocomponentcontent");
            model.setComponentContent("true".equalsIgnoreCase(contentAttr));

            Map<String, WOComponentApiBinding> bindingMap = new LinkedHashMap<>();

            NodeList children = woElem.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node node = children.item(i);
                if (node.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }
                Element elem = (Element) node;
                if ("binding".equals(elem.getTagName())) {
                    String name = elem.getAttribute("name");
                    String defaults = elem.hasAttribute("defaults") ? elem.getAttribute("defaults") : null;
                    WOComponentApiBinding binding = new WOComponentApiBinding(name, defaults, false, false);
                    bindingMap.put(name, binding);
                    model.addBinding(binding);
                } else if ("validation".equals(elem.getTagName())) {
                    boolean handled = false;
                    NodeList valChildren = elem.getChildNodes();
                    for (int j = 0; j < valChildren.getLength(); j++) {
                        Node vChild = valChildren.item(j);
                        if (vChild.getNodeType() != Node.ELEMENT_NODE) {
                            continue;
                        }
                        Element vElem = (Element) vChild;
                        String vTagName = vElem.getTagName();
                        String boundName = vElem.getAttribute("name");
                        if ("unbound".equals(vTagName) && boundName != null && !boundName.isBlank()) {
                            WOComponentApiBinding b = bindingMap.get(boundName);
                            if (b != null) {
                                b.setRequired(true);
                                String message = elem.getAttribute("message");
                                if (message != null && !message.isBlank()) {
                                    b.setRequiredMessage(message);
                                }
                                handled = true;
                            }
                        } else if ("unsettable".equals(vTagName) && boundName != null && !boundName.isBlank()) {
                            WOComponentApiBinding b = bindingMap.get(boundName);
                            if (b != null) {
                                b.setWillSet(true);
                                handled = true;
                            }
                        }
                    }
                    if (!handled) {
                        model.getExtraValidationsXml().add(elementToString(elem));
                    }
                }
            }
        } catch (Exception e) {
            // Return model populated so far
        }

        return model;
    }

    /**
     * Serializes the WOComponentApiModel into XML string conforming to WebObjects .api specification.
     */
    public static String toXml(WOComponentApiModel model) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n");
        sb.append("<wodefinitions>\n");
        sb.append("    <wo class=\"").append(escapeXml(model.getComponentClassName())).append("\" ");
        sb.append("wocomponentcontent=\"").append(model.isComponentContent() ? "true" : "false").append("\">\n");

        for (WOComponentApiBinding b : model.getBindings()) {
            sb.append("        <binding");
            if (b.getDefaults() != null && !b.getDefaults().isBlank()) {
                sb.append(" defaults=\"").append(escapeXml(b.getDefaults())).append("\"");
            }
            sb.append(" name=\"").append(escapeXml(b.getName())).append("\"/>\n");
        }

        // Required validations
        for (WOComponentApiBinding b : model.getBindings()) {
            if (b.isRequired() && b.getName() != null && !b.getName().isBlank()) {
                String msg = (b.getRequiredMessage() != null && !b.getRequiredMessage().isBlank())
                        ? b.getRequiredMessage()
                        : "'" + b.getName() + "' is a required binding.";
                sb.append("        <validation message=\"").append(escapeXml(msg)).append("\">\n")
                        .append("            <unbound name=\"").append(escapeXml(b.getName())).append("\"/>\n")
                        .append("        </validation>\n");
            }
        }

        // Will-set validations
        for (WOComponentApiBinding b : model.getBindings()) {
            if (b.isWillSet() && b.getName() != null && !b.getName().isBlank()) {
                sb.append("        <validation message=\"'").append(escapeXml(b.getName()))
                        .append("' must be bound to a settable value\">\n")
                        .append("            <unsettable name=\"").append(escapeXml(b.getName())).append("\"/>\n")
                        .append("        </validation>\n");
            }
        }

        // Extra validations preserved
        for (String extra : model.getExtraValidationsXml()) {
            sb.append("        ").append(extra.trim()).append("\n");
        }

        sb.append("    </wo>\n");
        sb.append("</wodefinitions>\n");

        return sb.toString();
    }

    private static String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("\"", "&quot;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;");
    }

    private static String elementToString(Element element) {
        try {
            TransformerFactory tf = TransformerFactory.newInstance();
            Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(element), new StreamResult(writer));
            return writer.toString();
        } catch (Exception e) {
            return "";
        }
    }
}
