package CustomOreGen.Client;

import java.util.List;
import java.util.function.Consumer;

import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.TabButton;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.navigation.ScreenAxis;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.network.chat.Component;

import CustomOreGen.Server.GuiCustomOreGenSettings;

/**
 * The "Custom Ore Generation..." button on the create world screen. (It was in ServerState, under @OnlyIn, which
 * NeoForge no longer strips; dedicated servers never load this class.)
 */
public class CreateWorldOptionsButton
{
    private static Button _optionsGuiButton = null;

    public static void addOptionsButtonToGui(CreateWorldScreen gui, List<GuiEventListener> buttonList, Consumer<GuiEventListener> add)
    {
        if (gui == null)
        {
            _optionsGuiButton = null;
        }
        else
        {
            GuiCustomOreGenSettings settingsGui = new GuiCustomOreGenSettings(gui);
            
            var action = new Button.OnPress() {
                @Override
                public void onPress(Button b) {
                	gui.getMinecraft().gui.setScreen(settingsGui);
                }
            };
            
            _optionsGuiButton = Button.builder(Component.literal("Custom Ore Generation..."), action).build();
            
            Button button1 = _optionsGuiButton;
            
            if (!buttonList.contains(button1))
            {
            	int n = gui.children().size();
            	
            	GuiEventListener b = gui.children().get(n-1);
            	
            	button1.setX(b.getRectangle().getCenterInAxis(ScreenAxis.HORIZONTAL));
            	button1.setY(b.getRectangle().bottom() + 1);
                add.accept(button1);
            }
        }
    }
    
    /**
     * Shows the button only on the World tab, under that tab's last widget. Done every frame (from the screen's
     * render event), so it follows tab changes made by mouse or keyboard, including the first.
     */
    public static void updateOptionsButtonVisibility(CreateWorldScreen gui) {
    	if (_optionsGuiButton == null || !(gui.renderables.get(0) instanceof TabNavigationBar tabBar))
    	{
    		return;
    	}
    	int selected = 0;
    	for (GuiEventListener child : tabBar.children())
    	{
    		if (child instanceof TabButton tab && tab.isSelected())
    		{
    			break;
    		}
    		selected++;
    	}
    	_optionsGuiButton.visible = selected == 1;
    	if (!_optionsGuiButton.visible)
    	{
    		return;
    	}

    	_optionsGuiButton.setX(tabBar.getRectangle().getCenterInAxis(ScreenAxis.HORIZONTAL) - _optionsGuiButton.getWidth()/2);

    	// The tab's widgets come last (the tab manager adds them as it switches tabs); skip the button itself.
    	List<? extends GuiEventListener> children = gui.children();
    	for (int i = children.size() - 1; i >= 0; i--)
    	{
    		if (children.get(i) != _optionsGuiButton)
    		{
    			_optionsGuiButton.setY(children.get(i).getRectangle().bottom() + 1);
    			break;
    		}
    	}
    }
}
