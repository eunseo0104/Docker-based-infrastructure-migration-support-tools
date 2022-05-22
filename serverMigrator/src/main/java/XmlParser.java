import org.w3c.dom.Document;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;

public class XmlParser {

    public static Document loadXMLFromString(String xml) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        InputSource is = new InputSource(new StringReader(xml));
        return builder.parse(is);
    }

    public static Document stringBuilderToDocument(StringBuilder response) throws Exception {
        String xml = response.toString();
        Document document = XmlParser.loadXMLFromString(xml);
        document.getDocumentElement().normalize();

        return document;
    }
}
