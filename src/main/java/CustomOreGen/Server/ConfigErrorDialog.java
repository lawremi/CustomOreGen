package CustomOreGen.Server;

import CustomOreGen.CustomOreGenBase;
import java.awt.Button;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.Point;
import java.awt.TextArea;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.BoxLayout;
import org.lwjgl.opengl.Display;

public class ConfigErrorDialog implements WindowListener, ActionListener
{
    private boolean _waiting = false;
    private Dialog _dialog = null;
    private Button _abort = null;
    private Button _retry = null;
    private Button _ignore = null;
    private int _returnVal = 0;

    public int showDialog(Frame parentWindow, Throwable error)
    {
        if (this._dialog != null)
        {
            throw new IllegalStateException("CustomOreGen Config Error Dialog is already open!");
        }
        else
        {
            this._dialog = new Dialog(parentWindow, "CustomOreGen Config Error", false);
            this._dialog.addWindowListener(this);
            TextArea text = new TextArea(this.getMessage(error), 30, 120, 1);
            text.setEditable(false);
            text.setBackground(Color.WHITE);
            text.setFont(new Font("Monospaced", 0, 12));
            this._dialog.add(text);
            Panel buttonPanel = new Panel();
            this._abort = new Button("Abort");
            this._abort.addActionListener(this);
            buttonPanel.add(this._abort);
            this._retry = new Button("Retry");
            this._retry.addActionListener(this);
            buttonPanel.add(this._retry);
            this._ignore = new Button("Ignore");
            this._ignore.addActionListener(this);
            buttonPanel.add(this._ignore);
            buttonPanel.setLayout(new BoxLayout(buttonPanel, 0));
            this._dialog.add(buttonPanel);
            this._dialog.setLayout(new BoxLayout(this._dialog, 1));
            this._dialog.pack();
            Point loc = parentWindow.getLocation();
            Dimension parentSize = parentWindow.getSize();
            Dimension size = this._dialog.getSize();
            loc.x += (parentSize.width - size.width) / 2;
            loc.y += (parentSize.height - size.height) / 2;
            this._dialog.setLocation(loc);
            this._waiting = true;
            this._returnVal = 0;
            this._dialog.setVisible(true);
            boolean usingLWJGL = CustomOreGenBase.isClassLoaded("org.lwjgl.opengl.Display");

            while (this._waiting)
            {
                if (usingLWJGL && Display.isCreated())
                {
                    Display.processMessages();
                }
            }

            this._abort = null;
            this._retry = null;
            this._ignore = null;
            this._dialog.setVisible(false);
            this._dialog.dispose();
            this._dialog = null;
            return this._returnVal;
        }
    }

    protected String getMessage(Throwable error)
    {
        StringBuilder msg = new StringBuilder();
        msg.append("CustomOreGen has detected an error while trying to load its config files.\n");
        msg.append("At this time you may: \n");
        msg.append("  (1) Abort loading and close Minecraft (click \'Abort\').\n");
        msg.append("  (2) Try to fix the error and then reload the config files (click \'Retry\').\n");
        msg.append("  (3) Ignore the error and continue without loading the config files (click \'Ignore\').\n");
        msg.append("It is strongly recommended that you do not ignore the error.\n");
        msg.append('\n');
        msg.append("------ Error Message ------\n\n");
        msg.append(error.toString());
        msg.append("\n\n");

        for (Throwable th = error.getCause(); th != null; th = th.getCause())
        {
            msg.append("-------- Caused By --------\n\n");
            msg.append(th.toString());
            msg.append("\n\n");
        }

        return msg.toString();
    }

    public void actionPerformed(ActionEvent e)
    {
        if (e.getSource() == this._abort)
        {
            this._returnVal = 0;
            this._waiting = false;
        }
        else if (e.getSource() == this._retry)
        {
            this._returnVal = 1;
            this._waiting = false;
        }
        else if (e.getSource() == this._ignore)
        {
            this._returnVal = 2;
            this._waiting = false;
        }
    }

    public void windowClosing(WindowEvent e)
    {
        this._waiting = false;
    }

    public void windowActivated(WindowEvent e) {}

    public void windowClosed(WindowEvent e) {}

    public void windowDeactivated(WindowEvent e) {}

    public void windowDeiconified(WindowEvent e) {}

    public void windowIconified(WindowEvent e) {}

    public void windowOpened(WindowEvent e) {}
}
