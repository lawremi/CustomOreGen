package CustomOreGen.Server;

import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiSlider;
import net.minecraft.client.gui.GuiSlot;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.settings.EnumOptions;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class GuiCustomOreGenSettings extends GuiScreen
{
    protected final GuiScreen _parentGui;
    protected int refreshGui = 3;
    protected GuiButton _doneButton = null;
    protected GuiButton _resetButton = null;
    protected GuiCustomOreGenSettings.GuiOptionSlot _optionPanel = null;
    protected GuiGroupPanel _groupPanel = null;
    protected String _toolTip = null;

    public GuiCustomOreGenSettings(GuiScreen parentGui)
    {
        this._parentGui = parentGui;
    }

    public void initGui()
    {
        super.initGui();
        super.buttonList.clear();
        ConfigOption.DisplayGroup currentGroup = this._groupPanel == null ? null : this._groupPanel.getSelectedGroup();

        if (this.refreshGui >= 2)
        {
            if (this.refreshGui >= 3)
            {
                WorldConfig.loadedOptionOverrides[0] = null;
            }

            WorldConfig visibleGroups = null;

            while (visibleGroups == null)
            {
                try
                {
                    visibleGroups = new WorldConfig();
                }
                catch (Exception var7)
                {
                    if (ServerState.onConfigError(var7))
                    {
                        visibleGroups = null;
                        continue;
                    }

                    visibleGroups = WorldConfig.createEmptyConfig();
                }

                WorldConfig.loadedOptionOverrides[0] = visibleGroups.getConfigOptions();

                if (currentGroup != null)
                {
                    ConfigOption visibleOptions = visibleGroups.getConfigOption(currentGroup.getName());

                    if (visibleOptions instanceof ConfigOption.DisplayGroup)
                    {
                        currentGroup = (ConfigOption.DisplayGroup)visibleOptions;
                    }
                }
            }
        }

        this.refreshGui = 0;
        Vector visibleGroups1 = new Vector();
        Vector visibleOptions1 = new Vector();
        
        nextOption: for (ConfigOption option : WorldConfig.loadedOptionOverrides[0]) {
        	
            if (option.getDisplayState() != null && option.getDisplayState() != ConfigOption.DisplayState.hidden)
            {
                ConfigOption.DisplayGroup group;

                if (option instanceof ConfigOption.DisplayGroup)
                {
                    for (group = currentGroup; group != option.getDisplayGroup(); group = group.getDisplayGroup())
                    {
                        if (group == null)
                        {
                            continue nextOption;
                        }
                    }

                    visibleGroups1.add((ConfigOption.DisplayGroup)option);
                }
                else
                {
                    for (group = option.getDisplayGroup(); group != currentGroup; group = group.getDisplayGroup())
                    {
                        if (group == null)
                        {
                            continue nextOption;
                        }
                    }

                    visibleOptions1.add(option);
                }
            }
        }

        if (!visibleGroups1.isEmpty())
        {
            this._groupPanel = new GuiGroupPanel(0, 20, super.width, 20, currentGroup, visibleGroups1);
        }
        else
        {
            this._groupPanel = null;
        }

        this._optionPanel = new GuiOptionSlot(visibleGroups1.isEmpty() ? 16 : 40, super.height - 30, 25, visibleOptions1);
        this._optionPanel.registerScrollButtons(1, 2);
        this._doneButton = new GuiButton(0, super.width / 2 - 155, super.height - 24, 150, 20, "Done");
        this._resetButton = new GuiButton(0, super.width / 2 + 5, super.height - 24, 150, 20, "Defaults");
        super.buttonList.add(this._doneButton);
        super.buttonList.add(this._resetButton);
    }

    protected void actionPerformed(GuiButton button)
    {
        super.actionPerformed(button);

        if (button == this._doneButton)
        {
            super.mc.displayGuiScreen(this._parentGui);
        }
        else if (button == this._resetButton)
        {
            this.refreshGui = 3;
        }
        else
        {
            this._optionPanel.actionPerformed(button);
        }
    }

    protected void mouseClicked(int mouseX, int mouseY, int buttonID)
    {
        super.mouseClicked(mouseX, mouseY, buttonID);
        this._groupPanel.mouseClicked(mouseX, mouseY, buttonID);
    }

    public void drawScreen(int mouseX, int mouseY, float partialTick)
    {
        this.drawDefaultBackground();
        this._optionPanel.drawScreen(mouseX, mouseY, partialTick);
        this._groupPanel.drawScreen(mouseX, mouseY, partialTick);
        this.drawCenteredString(super.fontRenderer, "CustomOreGen Options", super.width / 2, 4, 16777215);
        super.drawScreen(mouseX, mouseY, partialTick);

        if (this._toolTip != null)
        {
            List<String> lines = super.fontRenderer.listFormattedStringToWidth(this._toolTip, 2 * super.width / 5 - 8);
            int[] lineWidths = new int[lines.size()];
            int tipW = 0;
            int tipH = 8 + super.fontRenderer.FONT_HEIGHT * lines.size();
            int l = 0;

            for (Iterator tipX = lines.iterator(); tipX.hasNext(); ++l)
            {
                String tipY = (String)tipX.next();
                lineWidths[l] = super.fontRenderer.getStringWidth(tipY) + 8;

                if (tipW < lineWidths[l])
                {
                    tipW = lineWidths[l];
                }
            }

            int var13 = mouseX;
            int var14 = mouseY;

            if (mouseX > 2 * super.width / 5)
            {
                var13 = mouseX - tipW;
            }

            if (mouseY > super.height / 2)
            {
                var14 = mouseY - tipH;
            }

            this.drawGradientRect(var13, var14, var13 + tipW, var14 + tipH, -15724528, -14671840);
            l = 0;

            for (String line : lines) {
            	super.fontRenderer.drawString(line, var13 + (tipW - lineWidths[l]) / 2 + 4, var14 + 4 + l * super.fontRenderer.FONT_HEIGHT, 16777215);
            	++l;
            }

            this._toolTip = null;
        }

        if (this.refreshGui > 0)
        {
            this.initGui();
        }
    }
     
    public class GuiGroupPanel
    {
        protected int posX;
        protected int posY;
        protected int width;
        protected int height;
        protected int _scrollHInset;
        protected int _scrollOffsetX;
        protected int _scrollOffsetMax;
        protected int mouseX;
        protected int mouseY;
        protected final Vector _groupButtons;
        protected GuiButton _groupScrollLButton;
        protected GuiButton _groupScrollRButton;
        protected IOptionControl _currentGroup;
        protected GuiButton _currentButton;

        public GuiGroupPanel(int x, int y, int width, int height, ConfigOption.DisplayGroup selGroup, Vector groups)
        {
            this.posX = 0;
            this.posY = 0;
            this.width = 0;
            this.height = 0;
            this._scrollHInset = 0;
            this._scrollOffsetX = 0;
            this._scrollOffsetMax = 0;
            this.mouseX = 0;
            this.mouseY = 0;
            this._groupButtons = new Vector();
            this._groupScrollLButton = null;
            this._groupScrollRButton = null;
            this._currentGroup = null;
            this._currentButton = null;
            this.posX = x;
            this.posY = y;
            this.width = width;
            this.height = height;
            int scrollWidth = 0;
            int curSelX = 0;

            for (int c = -1; !groups.isEmpty() && c < groups.size(); ++c)
            {
                ConfigOption.DisplayGroup group = c < 0 ? null : (ConfigOption.DisplayGroup)groups.get(c);
                String text = c < 0 ? "[ All ]" : group.getDisplayName();
                int btnWidth = fontRenderer.getStringWidth(text) + 10;
                GuiGroupButton control = new GuiGroupButton(c + 1, scrollWidth, 0, btnWidth, height, text, group);
                this._groupButtons.add(control);

                if (group == selGroup)
                {
                    curSelX = (width - btnWidth) / 2 - scrollWidth;
                    this._currentGroup = control;
                }

                scrollWidth += btnWidth;
            }

            this._scrollHInset = 4;
            this._scrollOffsetMax = width - 2 * this._scrollHInset - scrollWidth;

            if (this._scrollOffsetMax < 0)
            {
                this._groupScrollLButton = new GuiButton(0, x + 4, y, 20, height, "<");
                this._groupScrollRButton = new GuiButton(0, x + width - 24, y, 20, height, ">");
                this._scrollHInset = 26;
                this._scrollOffsetMax = width - 2 * this._scrollHInset - scrollWidth;
                this._scrollOffsetX = Math.min(Math.max(curSelX - this._scrollHInset, this._scrollOffsetMax), 0);
            }
            else
            {
                this._scrollOffsetX = this._scrollOffsetMax / 2;
            }
        }

        public ConfigOption.DisplayGroup getSelectedGroup()
        {
            return this._currentGroup == null ? null : (ConfigOption.DisplayGroup)this._currentGroup.getOption();
        }

        public int getScrollPos()
        {
            return this._scrollOffsetX;
        }

        public boolean isInScrollArea(int x, int y)
        {
            int rx = x - this.posX;
            int ry = y - this.posY;
            return rx >= this._scrollHInset && rx <= this.width - this._scrollHInset && ry > 0 && ry < this.height;
        }

        public void drawScreen(int mouseX, int mouseY, float partialTick)
        {
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            Iterator tess = this._groupButtons.iterator();

            while (tess.hasNext())
            {
                IOptionControl texSize = (IOptionControl)tess.next();
                texSize.getControl().drawButton(mc, mouseX, mouseY);
            }

            Tessellator tess1 = Tessellator.instance;
            GL11.glDisable(2929);
            mc.func_110434_K().func_110577_a(Gui.field_110325_k);
            GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
            double texSize1 = 32.0D;
            tess1.startDrawingQuads();
            tess1.setColorRGBA_I(4473924, 255);
            tess1.addVertexWithUV((double)this.posX, (double)(this.posY + this.height), 0.0D, 0.0D, (double)this.height / texSize1);
            tess1.addVertexWithUV((double)(this.posX + this._scrollHInset), (double)(this.posY + this.height), 0.0D, (double)this._scrollHInset / texSize1, (double)this.height / texSize1);
            tess1.addVertexWithUV((double)(this.posX + this._scrollHInset), (double)this.posY, 0.0D, (double)this._scrollHInset / texSize1, 0.0D);
            tess1.addVertexWithUV((double)this.posX, (double)this.posY, 0.0D, 0.0D, 0.0D);
            tess1.addVertexWithUV((double)(this.posX + this.width - this._scrollHInset), (double)(this.posY + this.height), 0.0D, 0.0D, (double)this.height / texSize1);
            tess1.addVertexWithUV((double)(this.posX + this.width), (double)(this.posY + this.height), 0.0D, (double)this._scrollHInset / texSize1, (double)this.height / texSize1);
            tess1.addVertexWithUV((double)(this.posX + this.width), (double)this.posY, 0.0D, (double)this._scrollHInset / texSize1, 0.0D);
            tess1.addVertexWithUV((double)(this.posX + this.width - this._scrollHInset), (double)this.posY, 0.0D, 0.0D, 0.0D);
            tess1.draw();

            if (this._groupScrollLButton != null)
            {
                this._groupScrollLButton.drawButton(mc, mouseX, mouseY);

                if (this._groupScrollLButton == this._currentButton)
                {
                    this._scrollOffsetX = Math.min(0, this._scrollOffsetX + 1);
                }
            }

            if (this._groupScrollRButton != null)
            {
                this._groupScrollRButton.drawButton(mc, mouseX, mouseY);

                if (this._groupScrollRButton == this._currentButton)
                {
                    this._scrollOffsetX = Math.max(this._scrollOffsetMax, this._scrollOffsetX - 1);
                }
            }

            if (this._currentButton != null && !Mouse.isButtonDown(0))
            {
                this._currentButton = null;
            }
        }

        public void mouseClicked(int mouseX, int mouseY, int buttonID)
        {
            this.mouseX = mouseX;
            this.mouseY = mouseY;

            if (buttonID == 0)
            {
                this._currentButton = null;
                Iterator i$ = this._groupButtons.iterator();

                while (i$.hasNext())
                {
                    IOptionControl control = (IOptionControl)i$.next();

                    if (control.getControl().mousePressed(mc, mouseX, mouseY))
                    {
                        this._currentButton = control.getControl();
                        mc.sndManager.playSoundFX("random.click", 1.0F, 1.0F);
                        break;
                    }
                }

                if (this._groupScrollLButton != null && this._groupScrollLButton.mousePressed(mc, mouseX, mouseY))
                {
                    this._currentButton = this._groupScrollLButton;
                }
                else if (this._groupScrollRButton != null && this._groupScrollRButton.mousePressed(mc, mouseX, mouseY))
                {
                    this._currentButton = this._groupScrollRButton;
                }
            }
        }
        
        class GuiGroupButton extends GuiButton implements IOptionControl
        {
            protected final ConfigOption.DisplayGroup _group;
            private final int _relX;
            private final int _relY;

            public GuiGroupButton(int id, int relX, int relY, int width, int height, String text, ConfigOption.DisplayGroup group)
            {
                super(id, relX, relY, width, height, text);
                this._group = group;
                this._relX = relX;
                this._relY = relY;
            }

            public ConfigOption getOption()
            {
                return this._group;
            }

            public GuiButton getControl()
            {
                return this;
            }

            private boolean isButtonVisible()
            {
                super.xPosition = posX + _scrollHInset + _scrollOffsetX + this._relX;
                super.yPosition = posY + this._relY;
                return !isInScrollArea(super.xPosition, super.yPosition) && !isInScrollArea(super.xPosition + super.width, super.yPosition + super.height);
            }

            public boolean mousePressed(Minecraft mc, int mouseX, int mouseY)
            {
                if (this.isButtonVisible() && this.func_82252_a() && super.mousePressed(mc, mouseX, mouseY))
                {
                    _currentGroup = this;
                    refreshGui = 1;
                    return true;
                }
                else
                {
                    return false;
                }
            }

            public int getHoverState(boolean mouseOver)
            {
                if (isInScrollArea(mouseX, mouseY))
                {
                    return super.getHoverState(mouseOver);
                }
                else
                {
                    super.field_82253_i = false;
                    return 1;
                }
            }

            public void drawButton(Minecraft mc, int mouseX, int mouseY)
            {
                if (this.isButtonVisible() && super.drawButton)
                {
                    super.field_82253_i = mouseX >= super.xPosition && mouseY >= super.yPosition && mouseX < super.xPosition + super.width && mouseY < super.yPosition + super.height;
                    int hoverState = this.getHoverState(super.field_82253_i);
                    this.mouseDragged(mc, mouseX, mouseY);

                    if (_currentGroup == this)
                    {
                        drawRect(super.xPosition, super.yPosition, super.xPosition + super.width, super.yPosition + super.height, -1610612736);
                    }
                    else
                    {
                        if (this._group == null)
                        {
                            drawRect(super.xPosition, super.yPosition, super.xPosition + 1, super.yPosition + super.height, -16777216);
                        }

                        drawRect(super.xPosition, super.yPosition, super.xPosition + super.width, super.yPosition + 1, -16777216);
                        drawRect(super.xPosition + super.width - 1, super.yPosition, super.xPosition + super.width, super.yPosition + super.height, -16777216);
                    }

                    FontRenderer fontRenderer = mc.fontRenderer;
                    int textColor = super.enabled ? (super.field_82253_i ? 16777120 : 14737632) : 10526880;
                    this.drawCenteredString(fontRenderer, super.displayString, super.xPosition + super.width / 2, super.yPosition + (super.height - 8) / 2, textColor);

                    if (this.func_82252_a() && this._group != null)
                    {
                        _toolTip = this._group.getDescription();
                    }
                }
            }
        }

    }

    
    public class GuiOptionSlot extends GuiSlot
    {
        protected final Vector _optionControls;
        protected IOptionControl _clickTarget;

        public GuiOptionSlot(int top, int bottom, int slotHeight, Vector options)
        {
            super(mc, width, height, top, bottom, slotHeight);
            this._optionControls = new Vector();
            this._clickTarget = null;

            for (int c = 0; c < options.size(); ++c)
            {
                ConfigOption option = (ConfigOption)options.get(c);
                Object control = null;

                if (option instanceof ChoiceOption)
                {
                    this._optionControls.add(new GuiChoiceButton(this, c, 2 * width / 5 + 15, 0, width / 10 + 100, slotHeight - 6, (ChoiceOption)option));
                }
                else if (option instanceof NumericOption)
                {
                    this._optionControls.add(new GuiNumericSlider(this, c, 2 * width / 5 + 15, 0, width / 10 + 100, slotHeight - 6, (NumericOption)option));
                }
            }
        }

        protected int getSize()
        {
            return this._optionControls.size();
        }

        protected void elementClicked(int index, boolean doubleClicked)
        {
            if (this._clickTarget != null)
            {
                this._clickTarget.getControl().mouseReleased(super.mouseX, super.mouseY);

                if (this._clickTarget.getOption().getDisplayState() == ConfigOption.DisplayState.shown_dynamic)
                {
                    refreshGui = 2;
                }

                this._clickTarget = null;
            }

            IOptionControl control = (IOptionControl)this._optionControls.get(index);

            if (control.getControl().mousePressed(mc, super.mouseX, super.mouseY))
            {
                mc.sndManager.playSoundFX("random.click", 1.0F, 1.0F);
                this._clickTarget = control;
            }
        }

        protected boolean isSelected(int var1)
        {
            return false;
        }

        public boolean isInBounds(int x, int y)
        {
            return super.mouseY >= super.top && super.mouseY <= super.bottom && super.mouseX >= 0 && super.mouseX <= width;
        }

        protected void drawBackground() {}

        protected void drawSlot(int index, int slotX, int slotY, int slotH, Tessellator tess)
        {
            IOptionControl optCtrl = (IOptionControl)this._optionControls.get(index);
            ConfigOption option = optCtrl.getOption();
            GuiButton control = optCtrl.getControl();
            String optionName = option.getDisplayName();
            int nameW = fontRenderer.getStringWidth(optionName);
            int nameX = 2 * width / 5 - 15 - nameW;
            int nameH = fontRenderer.FONT_HEIGHT;
            int nameY = slotY + 8;
            drawString(fontRenderer, optionName, nameX, nameY, 16777215);

            if (super.mouseX >= nameX && super.mouseX <= nameX + nameW && super.mouseY >= nameY && super.mouseY <= nameY + nameH && this.isInBounds(super.mouseX, super.mouseY))
            {
                _toolTip = option.getDescription();
            }

            control.yPosition = slotY + 3;
            control.drawButton(mc, super.mouseX, super.mouseY);
        }

        public void drawScreen(int mouseX, int mouseY, float partialTick)
        {
            super.drawScreen(mouseX, mouseY, partialTick);

            if (this._clickTarget != null && !Mouse.isButtonDown(0))
            {
                this._clickTarget.getControl().mouseReleased(mouseX, mouseY);

                if (this._clickTarget.getOption().getDisplayState() == ConfigOption.DisplayState.shown_dynamic)
                {
                    refreshGui = 2;
                }

                this._clickTarget = null;
            }
        }
        
        class GuiChoiceButton extends GuiButton implements IOptionControl
        {
            private final ChoiceOption _choice;
            private final int _maxWidth;
            private int mouseX;
            private int mouseY;

            final GuiOptionSlot this$1;

            public GuiChoiceButton(GuiOptionSlot var1, int id, int x, int y, int maxWidth, int height, ChoiceOption choice)
            {
                super(id, x, y, maxWidth, height, (String)null);
                this.this$1 = var1;
                this.mouseX = 0;
                this.mouseY = 0;
                this._choice = choice;
                this._maxWidth = maxWidth;
                this.onValueChanged();
            }

            public ConfigOption getOption()
            {
                return this._choice;
            }

            public GuiButton getControl()
            {
                return this;
            }

            protected void onValueChanged()
            {
                super.displayString = this._choice.getDisplayValue();
                int strWidth = super.displayString == null ? 0 : fontRenderer.getStringWidth(super.displayString);
                super.width = Math.min(this._maxWidth, strWidth + 10);
            }

            public boolean mousePressed(Minecraft mc, int mouseX, int mouseY)
            {
                if (super.mousePressed(mc, mouseX, mouseY))
                {
                    this._choice.setValue(this._choice.nextPossibleValue());
                    this.onValueChanged();
                    return true;
                }
                else
                {
                    return false;
                }
            }

            public int getHoverState(boolean mouseOver)
            {
                if (this.this$1.isInBounds(this.mouseX, this.mouseY))
                {
                    return super.getHoverState(mouseOver);
                }
                else
                {
                    super.field_82253_i = false;
                    return 1;
                }
            }

            public void drawButton(Minecraft mc, int mouseX, int mouseY)
            {
                super.drawButton(mc, mouseX, mouseY);

                if (this.func_82252_a())
                {
                    _toolTip = this._choice.getValueDescription();
                }
            }
        }

        class GuiNumericSlider extends GuiSlider implements IOptionControl
        {
            private final NumericOption _numeric;

            final GuiOptionSlot this$1;

            public GuiNumericSlider(GuiOptionSlot var1, int id, int x, int y, int width, int height, NumericOption numeric)
            {
                super(id, x, y, EnumOptions.ANAGLYPH, (String)null, (float)numeric.getNormalizedDisplayValue());
                this.this$1 = var1;
                this._numeric = numeric;
                super.width = width;
                super.height = height;
                this.onValueChanged();
            }

            public ConfigOption getOption()
            {
                return this._numeric;
            }

            public GuiButton getControl()
            {
                return this;
            }

            protected void onValueChanged()
            {
                long prec = 6L;
                long base = (long)Math.pow(10.0D, (double)prec);

                for (long val = Math.round(this._numeric.getDisplayIncr() * (double)base); prec > 0L && val % 10L == 0L; --prec)
                {
                    val /= 10L;
                    base /= 10L;
                }

                super.displayString = String.format("%." + prec + "f", new Object[] {Double.valueOf(this._numeric.getDisplayValue())});
            }

            public boolean mousePressed(Minecraft mc, int mouseX, int mouseY)
            {
                if (super.mousePressed(mc, mouseX, mouseY))
                {
                    this._numeric.setNormalizedDisplayValue((double)super.sliderValue);
                    this.onValueChanged();
                    return true;
                }
                else
                {
                    return false;
                }
            }

            protected void mouseDragged(Minecraft mc, int mouseX, int mouseY)
            {
                super.mouseDragged(mc, mouseX, mouseY);
                this._numeric.setNormalizedDisplayValue((double)super.sliderValue);
                this.onValueChanged();
            }
        }

    }

    public static class GuiOpenMenuButton extends GuiButton
    {
        protected final GuiScreen _parentGui;
        protected final GuiScreen _targetGui;

        public GuiOpenMenuButton(GuiScreen parentGui, int id, int x, int y, int width, int height, String text, GuiScreen openedGui)
        {
            super(id, x, y, width, height, text);
            this._parentGui = parentGui;
            this._targetGui = openedGui;
        }

        public int getWidth()
        {
            return super.width;
        }

        public boolean mousePressed(Minecraft mc, int mouseX, int mouseY)
        {
            if (super.mousePressed(mc, mouseX, mouseY))
            {
                mc.displayGuiScreen(this._targetGui);
                return true;
            }
            else
            {
                return false;
            }
        }
    }

    public interface IOptionControl
    {
        ConfigOption getOption();

        GuiButton getControl();
    }

}
