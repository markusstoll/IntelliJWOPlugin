package org.wocommunity.plugins.intellij;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ProjectUtil {

    public static final String ORG_MAVEN_IDE_ECLIPSE_MAVEN_2_NATURE = "org.maven.ide.eclipse.maven2Nature";

    public static String getModulePath(@NotNull Module module)
    {
        String modulePath = ModuleUtil.getModuleDirPath(module);
        if(modulePath.endsWith(".idea"))
        {
            modulePath = modulePath.substring(0, modulePath.length()-("/.idea".length()));
        }

        return modulePath;
    }

    public void createOrUpdateProjectDescriptionFile(File projectDir)  {
        File projectDescriptionFile = new File(projectDir, ".project");

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document document = builder.parse(projectDescriptionFile);
            boolean mavenNatureFound = false;

            NodeList projectDescriptionList = document.getElementsByTagName("projectDescription");
            if(projectDescriptionList.getLength() == 1)
            {
                Node naturesElement = getChildWithName(projectDescriptionList.item(0), "natures");
                if(naturesElement != null) {
                    NodeList natureList = naturesElement.getChildNodes();

                    for (int i = 0; i < natureList.getLength(); i++) {
                        Node natureElement = (Node) natureList.item(i);
                        if (natureElement.getNodeName().equals("nature")
                                && ORG_MAVEN_IDE_ECLIPSE_MAVEN_2_NATURE.equals(natureElement.getTextContent())) {
                            mavenNatureFound = true;

                            return; // nothing to do
                        }
                    }

                    Element newNatureElement = document.createElement("nature");
                    newNatureElement.setTextContent(ORG_MAVEN_IDE_ECLIPSE_MAVEN_2_NATURE);

                    naturesElement.appendChild(newNatureElement);

                    writeDocumentToFile(document, projectDescriptionFile);
                    return;
                }
            }

        } catch (IOException | ParserConfigurationException e) {
        } catch (SAXException e) {
        }

        try {
            // File does not exist, create it
            createProjectDescriptionFile(projectDescriptionFile);
        } catch (IOException |ParserConfigurationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private Node getChildWithName(Node node, String name)
    {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if ("natures".equals(child.getNodeName())) {
                return child;
            }
        }

        return null;
    }

    private void createProjectDescriptionFile(File target) throws IOException, ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document document = builder.newDocument();
        Element rootElement = document.createElement("projectDescription");
        document.appendChild(rootElement);

        Element naturesElement = document.createElement("natures");
        rootElement.appendChild(naturesElement);

        Element natureElement = document.createElement("nature");
        natureElement.setTextContent(ORG_MAVEN_IDE_ECLIPSE_MAVEN_2_NATURE);

        naturesElement.appendChild(natureElement);

        writeDocumentToFile(document, target);
    }

    public static void writeDocumentToFile(Document document, File target) {
        try {
            // Set up a transformer for converting Document to XML
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            // Set up the source and result
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(target);

            // Transform the Document to the file
            transformer.transform(source, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String documentToString(Document document) {
        StringBuilder builder = new StringBuilder();

        if (document != null && document.getDocumentElement() != null) {
            builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            Node root = document.getDocumentElement();
            while (root != null) {
                builder.append("<").append(root.getNodeName()).append(">");
                NodeList children = root.getChildNodes();
                for (int i = 0; i < children.getLength(); i++) {
                    Node child = children.item(i);
                    if (child instanceof Element) {
                        builder.append(documentToString((Document) child.getOwnerDocument()));
                    } else {
                        builder.append(child.getNodeValue());
                    }
                }
                builder.append("</").append(root.getNodeName()).append(">\n");
                root = root.getNextSibling();
            }
        }

        return builder.toString();
    }
}