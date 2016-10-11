package CustomOreGen.Config;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;
import java.util.regex.Pattern;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.UserDataHandler;

public class ValidatorImport extends ValidatorNode
{
    protected boolean required = true;

    protected ValidatorImport(ValidatorNode parent, Node node, boolean required)
    {
        super(parent, node);
        this.required = required;
    }

    protected boolean validateChildren() throws ParserException
    {
        super.validateChildren();
        File currentFile = (File)this.getNode().getOwnerDocument().getUserData("value");
        File baseDirectory = currentFile.getParentFile();
        String fileName = this.validateRequiredAttribute(String.class, "file", true);
        List<File> files = getMatchingFiles(baseDirectory, fileName);
        this.getNode().setUserData("validated", Boolean.valueOf(true), (UserDataHandler)null);
        this.checkChildrenValid();

        if (files.isEmpty())
        {
            if (this.required)
            {
                throw new ParserException("No files found matching \'" + fileName + "\'.", this.getNode());
            }

            this.replaceWithNode(new Node[0]);
        }
        else
        {
            ArrayList<Node> contents = new ArrayList<Node>(files.size());
            for (File file : files) {
            	Element importRoot = this.getNode().getOwnerDocument().createElement("ImportedDoc");
                this.getNode().appendChild(importRoot);
                importRoot.setUserData("value", file, (UserDataHandler)null);

                try
                {
                    this.getParser().saxParser.parse(file, new LineAwareSAXHandler(importRoot));
                }
                catch (Exception var10)
                {
                    throw new ParserException(var10.getMessage(), this.getNode(), var10);
                }

                (new ValidatorUnchecked(this, importRoot)).validate();
                contents.add(importRoot);

            }

            this.replaceWithNodeContents(contents.toArray(new Node[contents.size()]));
        }

        return false;
    }

    private static List<File> getMatchingFiles(File baseDir, String relPath)
    {
        List<File> files = Arrays.asList(new File[] {(baseDir == null ? new File("") : baseDir).getAbsoluteFile()});
        Stack<String> subPaths = new Stack<String>();

        for (File subPath = new File(relPath); subPath != null; subPath = subPath.getParentFile())
        {
            subPaths.push(subPath.getName());
        }

        while (!subPaths.isEmpty())
        {
            String subPath = subPaths.pop();
            LinkedList<File> nextFiles = new LinkedList<File>();
            for (File dir : files) {
                if (!subPath.contains("*") && !subPath.contains("?"))
                {
                    File f = new File(dir, subPath);

                    if (f.exists())
                    {
                        nextFiles.add(f);
                    }
                }
                else if (dir.isDirectory())
                {
                    File[] file = dir.listFiles(new WildcardFileFilter(subPath));
                    for (File file1 : file) {
                    	if (file1.exists())
                        {
                            nextFiles.add(file1);
                        }
                    }
                }
            }
            files = nextFiles;
        }

        return files;
    }
    
    public static class Factory implements IValidatorFactory<ValidatorImport>
    {
        private final boolean _required;

        public Factory(boolean required)
        {
            this._required = required;
        }

        public ValidatorImport createValidator(ValidatorNode parent, Node node)
        {
            return new ValidatorImport(parent, node, this._required);
        }
    }

    private static class WildcardFileFilter implements FilenameFilter
    {
        private Pattern _pattern;

        public WildcardFileFilter(String pattern)
        {
            this._pattern = Pattern.compile("\\Q" + pattern.replace("*", "\\E.*\\Q").replace("?", "\\E.\\Q") + "\\E");
        }

        public boolean accept(File dir, String fileName)
        {
            return this._pattern.matcher(fileName).matches();
        }
    }

}
