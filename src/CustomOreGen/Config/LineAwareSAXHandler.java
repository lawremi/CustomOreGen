package CustomOreGen.Config;

import java.util.Stack;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.w3c.dom.UserDataHandler;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

public class LineAwareSAXHandler extends DefaultHandler
{
    private Locator locator = null;
    private final Stack nodeStack = new Stack();

    public LineAwareSAXHandler(Node root)
    {
        this.nodeStack.push(root);
    }

    public void setDocumentLocator(Locator locator)
    {
        this.locator = locator;
    }

    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException
    {
        Node current = (Node)this.nodeStack.peek();
        Document doc = current.getNodeType() == 9 ? (Document)current : current.getOwnerDocument();
        int lineNumber = this.locator.getLineNumber();
        Element newElement = doc.createElementNS(uri, qName);
        newElement.setUserData("line-number", Integer.valueOf(lineNumber), (UserDataHandler)null);
        current.appendChild(newElement);

        for (int c = 0; c < attributes.getLength(); ++c)
        {
            Attr attr = doc.createAttributeNS(attributes.getURI(c), attributes.getQName(c));
            attr.setValue(attributes.getValue(c));
            attr.setUserData("line-number", Integer.valueOf(lineNumber), (UserDataHandler)null);
            newElement.setAttributeNodeNS(attr);
        }

        this.nodeStack.push(newElement);
    }

    public void endElement(String uri, String localName, String qName)
    {
        Node closedNode = (Node)this.nodeStack.pop();
    }

    public void characters(char[] ch, int start, int length) throws SAXException
    {
        Node current = (Node)this.nodeStack.peek();
        Document doc = current.getNodeType() == 9 ? (Document)current : current.getOwnerDocument();
        Text textNode = doc.createTextNode(new String(ch, start, length));
        textNode.setUserData("line-number", Integer.valueOf(this.locator.getLineNumber()), (UserDataHandler)null);
        current.appendChild(textNode);
    }

    public void fatalError(SAXParseException ex) throws ParserException
    {
        throw new ParserException(ex.getMessage(), (Node)this.nodeStack.peek(), this.locator.getLineNumber(), ex);
    }
}
