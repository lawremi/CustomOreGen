package CustomOreGen.Config;

import org.w3c.dom.Attr;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

public class ParserException extends SAXException
{
	private static final long serialVersionUID = 1L;
	public final Node node;
    public int lineNumber;

    public ParserException(String message, Node node, int lineNumber, Exception parent)
    {
        super(message, parent);
        this.node = node;
        this.lineNumber = lineNumber;
    }

    public ParserException(String message, Node node, int lineNumber)
    {
        super(message);
        this.node = node;
        this.lineNumber = lineNumber;
    }

    public ParserException(String message, Node node, Exception parent)
    {
        this(message, node, -1, parent);
    }

    public ParserException(String message, Node node)
    {
        this(message, node, -1);
    }

    public ParserException(String message, int lineNumber, Exception parent)
    {
        this(message, (Node)null, lineNumber, parent);
    }

    public ParserException(String message, int lineNumber)
    {
        this(message, (Node)null, lineNumber);
    }

    public ParserException(String message, Exception parent)
    {
        this(message, (Node)null, -1, parent);
    }

    public ParserException(String message)
    {
        this(message, (Node)null, -1);
    }

    public ParserException(Exception parent)
    {
        super(parent);
        this.node = null;
        this.lineNumber = -1;
    }

    public String getMessage()
    {
        String prefix = "CustomOreGen Config Error";
        return this.node == null ? (this.lineNumber < 0 ? prefix + ": " + super.getMessage() : prefix + " at [line " + this.lineNumber + "]: " + super.getMessage()) : (this.lineNumber < 0 ? prefix + " at " + formatNode(this.node, 16) + ": " + super.getMessage() : prefix + " at " + formatNode(this.node, 0) + " [line " + this.lineNumber + "]: " + super.getMessage());
    }

    public String toString()
    {
        StringBuilder out = new StringBuilder(this.getMessage());

        if (this.node != null)
        {
            Object cnode = this.node;
            Object lastVisibleNode = null;

            while (cnode != null)
            {
                out.append("\n  in ");
                out.append(formatNode((Node)cnode, 17));
                Node hiddenParent = (Node)((Node)cnode).getUserData("hidden-parent");

                if (hiddenParent != null)
                {
                    if (lastVisibleNode == null)
                    {
                        lastVisibleNode = cnode;
                    }

                    cnode = hiddenParent;
                }
                else
                {
                    if (lastVisibleNode != null)
                    {
                        cnode = lastVisibleNode;
                        lastVisibleNode = null;
                    }

                    if (((Node)cnode).getNodeType() == 2)
                    {
                        cnode = ((Attr)cnode).getOwnerElement();
                    }
                    else
                    {
                        cnode = ((Node)cnode).getParentNode();
                    }
                }
            }
        }

        return out.toString();
    }

    public static String formatNode(Node node, int flags)
    {
        StringBuilder buffer = new StringBuilder();
        String nameFormat = null;

        switch (node.getNodeType())
        {
            case 1:
                buffer.append("Element");
                nameFormat = "<%s>";
                break;

            case 2:
                buffer.append("Attribute");
                nameFormat = "\'%s\'";
                break;

            case 3:
                buffer.append("Text");
                break;

            case 4:
                buffer.append("CData");
                break;

            case 5:
                buffer.append("EntityRef");
                nameFormat = "\'%s\'";
                break;

            case 6:
                buffer.append("Entity");
                nameFormat = "\'%s\'";
                break;

            case 7:
                buffer.append("Instruction");
                nameFormat = "\'%s\'";
                break;

            case 8:
                buffer.append("Comment");
                break;

            case 9:
                buffer.append("Document");
                break;

            case 10:
                buffer.append("DocType");
                nameFormat = "\'%s\'";
                break;

            case 11:
                buffer.append("Fragment");
                break;

            case 12:
                buffer.append("Notation");
                nameFormat = "\'%s\'";
        }

        Object attributes;

        if ((flags & 2) != 0)
        {
            attributes = node.getUserData("validated");

            if (attributes == null)
            {
                buffer.append(" ?");
            }
            else if (!(attributes instanceof Boolean))
            {
                buffer.append(" (valid=" + attributes + ")");
            }
            else if (((Boolean)attributes).booleanValue())
            {
                buffer.append(" +");
            }
            else
            {
                buffer.append(" -");
            }
        }

        String child;
        String prefix;

        if (nameFormat != null)
        {
            buffer.append(" ");
            prefix = node.getPrefix();

            if (prefix == null)
            {
                prefix = "";
            }
            else
            {
                prefix = prefix + ":";
            }

            child = node.getLocalName();

            if (child == null)
            {
                child = node.getNodeName();
            }

            buffer.append(String.format(nameFormat, new Object[] {prefix + child}));
        }

        if ((flags & 16) != 0)
        {
            attributes = node.getUserData("line-number");

            if (attributes != null)
            {
                buffer.append(" [line " + attributes + "]");
            }
        }

        if ((flags & 4) != 0)
        {
            prefix = node.getNamespaceURI();

            if (prefix != null)
            {
                buffer.append(" (xmlns=" + prefix + ")");
            }
        }

        if ((flags & 8) != 0)
        {
            buffer.append(" {class=" + node.getClass().getSimpleName() + "}");
        }

        if ((flags & 1) != 0)
        {
            attributes = node.getUserData("value");

            if (attributes == null)
            {
                child = node.getNodeValue();

                if (child != null)
                {
                    attributes = "\'" + child.trim() + "\'";
                }
            }

            if (attributes != null)
            {
                buffer.append(" = " + attributes);
            }
        }

        if ((flags & 4096) != 0)
        {
            NamedNodeMap attrs = node.getAttributes();
            String result;

            for (int var8 = 0; attrs != null && var8 < attrs.getLength(); ++var8)
            {
                result = "\n" + formatNode(attrs.item(var8), flags);
                buffer.append(result.replace("\n", "\n  "));
            }

            for (Node ch = node.getFirstChild(); ch != null; ch = ch.getNextSibling())
            {
                result = "\n" + formatNode(ch, flags);
                buffer.append(result.replace("\n", "\n  "));
            }
        }

        return buffer.toString();
    }
}
