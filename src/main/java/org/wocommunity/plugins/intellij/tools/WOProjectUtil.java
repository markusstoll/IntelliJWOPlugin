package org.wocommunity.plugins.intellij.tools;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtil;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import org.apache.commons.io.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.io.IOException;

public class WOProjectUtil {

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

    public static VirtualFile getVirtualFileForSubfolder(Module module, String subfolderPath) {
        if (module == null || subfolderPath == null || subfolderPath.isEmpty()) {
            return null;
        }

        // Get the content roots for the module
        VirtualFile[] contentRoots = ModuleRootManager.getInstance(module).getContentRoots();

        for (VirtualFile contentRoot : contentRoots) {
            // Construct the full path to the subfolder
            String fullPath = contentRoot.getPath() + "/" + subfolderPath;

            // Find the VirtualFile for the full path
            VirtualFile subfolder = VirtualFileManager.getInstance().findFileByUrl("file://" + fullPath);

            if (subfolder != null && subfolder.isDirectory()) {
                return subfolder;
            }
        }

        // Subfolder not found
        return null;
    }

    public static boolean isMavenStyleProjectDir(File projectDir)
    {
        File mavenStyleProperties = new File(projectDir, "src/main/resources/Properties");
        return mavenStyleProperties.exists();
    }

    public static boolean isMavenStyleModule(Module m)
    {
        return isMavenStyleProjectDir(new File(getModulePath(m)));
    }

    public static void createNewWOComponent(Module module,
                                            String componentName,
                                            @NotNull String javaPackage,
                                            @NotNull String superClass,
                                            VirtualFile componentFolder,
                                            boolean createHtmlContents,
                                            boolean createApiFile) throws Exception {

        JavaFileCreator.createJavaFile(
                module,
                javaPackage,
                componentName,
                superClass
        );

        WOComponentFileCreator.createWOComponent(
                module,
                componentName,
                componentFolder,
                createHtmlContents,
                createApiFile
        );
    }

    public void createOrUpdateProjectDescriptionFile(File projectDir)  {
        File projectDescriptionFile = new File(projectDir, ".project");

        boolean isMavenStyleProject = isMavenStyleProjectDir(projectDir);

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document document = builder.parse(projectDescriptionFile);
            Node natureElementWithEclipseNature = null;

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
                            natureElementWithEclipseNature = natureElement;
                        }
                    }

                    if(!isMavenStyleProject)
                    {
                        if(natureElementWithEclipseNature != null)
                        {
                            // remove eclipse maven nature
                            naturesElement.removeChild(natureElementWithEclipseNature);

                            writeDocumentToFile(document, projectDescriptionFile);
                        }

                        return;
                    }

                    if(natureElementWithEclipseNature == null)
                    {
                        Element newNatureElement = document.createElement("nature");
                        newNatureElement.setTextContent(ORG_MAVEN_IDE_ECLIPSE_MAVEN_2_NATURE);

                        naturesElement.appendChild(newNatureElement);

                        writeDocumentToFile(document, projectDescriptionFile);

                        return;
                    }
                }
            }

        } catch (IOException | ParserConfigurationException e) {
        } catch (SAXException e) {
        }

        try {
            if(isMavenStyleProject) {
                // File does not exist, create it
                createProjectDescriptionFile(projectDescriptionFile);
            }
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

    public static void removeWhitespaceNodes(Node node) {
        NodeList childNodes = node.getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node child = childNodes.item(i);
            if (child.getNodeType() == Node.TEXT_NODE && child.getNodeValue().trim().isEmpty()) {
                node.removeChild(child);
                i--; // Decrement index to account for removed node
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                removeWhitespaceNodes(child); // Recurse into child elements
            }
        }
    }


    public static void writeDocumentToFile(Document document, File target) {
        try {
            // Set up a transformer for converting Document to XML
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();

            removeWhitespaceNodes(document.getDocumentElement());

            // Set up the source and result
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(target);

            // Transform the Document to the file, specifying output encoding and indenting
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "no");
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.DOCTYPE_PUBLIC, "yes");

            // Transform the Document to the file
            transformer.transform(source, result);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String[] parseClassName(String fullyQualifiedName) {
        if (fullyQualifiedName == null || fullyQualifiedName.isEmpty()) {
            throw new IllegalArgumentException("Fully qualified class name cannot be null or empty.");
        }

        int lastDotIndex = fullyQualifiedName.lastIndexOf('.');

        if (lastDotIndex == -1) {
            // No dot means no package, return the class name as the simple name
            return new String[]{"", fullyQualifiedName};
        }

        String packageName = fullyQualifiedName.substring(0, lastDotIndex);
        String simpleClassName = fullyQualifiedName.substring(lastDotIndex + 1);

        return new String[]{packageName, simpleClassName};
    }
}