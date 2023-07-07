package CustomOreGen.Server;

import java.util.ArrayList;
import java.util.Vector;
import java.util.function.Consumer;

import com.mojang.blaze3d.vertex.PoseStack;

import CustomOreGen.Server.ConfigOption.DisplayGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.components.tabs.GridLayoutTab;
import net.minecraft.client.gui.components.tabs.TabManager;
import net.minecraft.client.gui.components.tabs.TabNavigationBar;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.LiteralContents;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GuiCustomOreGenSettings extends Screen
{
	protected final Screen _parentGui;
	protected int refreshGui = 2;
	protected Button _doneButton = null;
	protected Button _resetButton = null;
	protected GuiCustomOreGenSettings.GuiOptionSlot _optionPanel = null;
	//protected GuiGroupPanel _groupPanel = null;
	protected String _toolTip = null;

	private final TabManager tabManager = new TabManager(this::addRenderableWidget, (p_267853_) -> {
		this.removeWidget(p_267853_);
	});
	private TabNavigationBar tabNavigationBar;

	public GuiCustomOreGenSettings(Screen parentGui)
	{
		super(MutableComponent.create(new LiteralContents("Custom Ore Gen")));
		this._parentGui = parentGui;
	}

	@Override
	public void init()
	{
		super.init();
		super.clearWidgets();
		//ConfigOption.DisplayGroup currentGroup = this._groupPanel == null ? null : this._groupPanel.getSelectedGroup();

		ConfigOption.DisplayGroup currentGroup = tabNavigationBar == null ? null : ((DisplayGroupTab)tabManager.getCurrentTab()).group;
		
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

				WorldConfig.loadedOptionOverrides[0] = new ArrayList<ConfigOption>();
				for (ConfigOption option : visibleGroups.getConfigOptions()) {
					if (option.getDisplayState() != null && option.getDisplayState() != ConfigOption.DisplayState.hidden)
					{
						WorldConfig.loadedOptionOverrides[0].add(option);
					}
				}

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
		Vector<ConfigOption.DisplayGroup> visibleGroups1 = new Vector<DisplayGroup>();
		Vector<ConfigOption> visibleOptions1 = new Vector<ConfigOption>();
		
		TabNavigationBar.Builder bld = TabNavigationBar.builder(this.tabManager, this.width);
		/*.addTabs(
				new GuiCustomOreGenSettings.GameTab(),
				new CreateWorldScreen.WorldTab(),
				new CreateWorldScreen.MoreTab()
			);*/
		
		nextOption: for (ConfigOption option : WorldConfig.loadedOptionOverrides[0]) {

			ConfigOption.DisplayGroup group;

			if (option instanceof ConfigOption.DisplayGroup)
			{
				for (group = currentGroup; checkGroup(group, currentGroup); group = group.getDisplayGroup())
				{
					if (group == null)
					{
						continue nextOption;
					}
				}

				visibleGroups1.add((ConfigOption.DisplayGroup)option);
				bld.addTabs(new GuiCustomOreGenSettings.DisplayGroupTab((ConfigOption.DisplayGroup)option));
			}
			else
			{
				for (group = option.getDisplayGroup(); checkGroup(group, currentGroup); group = group.getDisplayGroup())
				{
					if (group == null)
					{
						continue nextOption;
					}
				}

				visibleOptions1.add(option);
			}
		}

		if(tabNavigationBar == null) {
			this.tabNavigationBar = bld.build();
			this.tabNavigationBar.selectTab(0, false);
		}
		this.addRenderableWidget(this.tabNavigationBar);
		//this._groupPanel = new GuiGroupPanel(0, 20, super.width, 20, currentGroup, visibleGroups1, this);
		this._optionPanel = new GuiOptionSlot(Minecraft.getInstance(), visibleGroups1.isEmpty() ? 16 : 40, super.height - 30,  25, visibleOptions1, this);
		//TODO: fix buttons
		//this._optionPanel.registerScrollButtons(1, 2);
		if(_doneButton == null) {
			this._doneButton = Button.builder(makeComponent("Done"), this::actionPerformed)
					.pos(super.width / 2 - 155, super.height - 24)
					.size(150, 20).build();
			this._resetButton = Button.builder(makeComponent("Defaults"), this::actionPerformed)
					.pos(super.width / 2 +5, super.height - 24)
					.size(150, 20).build();
		}
		else {
			this.addWidget(_doneButton);
			this.addWidget(_resetButton);
		}
		this.repositionElements();
	}
	
	private boolean checkGroup(DisplayGroup group, DisplayGroup displayGroup) {
		if(group == null || displayGroup == null) return group != displayGroup;
		return !group.equals(displayGroup);
	}

	public void repositionElements() {
		if (this.tabNavigationBar != null) {
			this.tabNavigationBar.setWidth(this.width);
			this.tabNavigationBar.arrangeElements();
			int i = this.tabNavigationBar.getRectangle().bottom();
			ScreenRectangle screenrectangle = new ScreenRectangle(0, i, this.width, this.height - i);
			this.tabManager.setTabArea(screenrectangle);
		}
	}

	private Component makeComponent(String string) {
		return MutableComponent.create(new LiteralContents(string));
	}

	@Override
	public boolean mouseClicked(double p_mouseClicked_1_, double p_mouseClicked_3_, int p_mouseClicked_5_) {
		for(GuiEventListener iguieventlistener : this.children()) {
			if (iguieventlistener.mouseClicked(p_mouseClicked_1_, p_mouseClicked_3_, p_mouseClicked_5_)) {
				this.setFocused(iguieventlistener);
				if (p_mouseClicked_5_ == 0) {
					this.setDragging(true);
				}

				return true;
			}
		}
		//this._optionPanel.mouseClicked(p_mouseClicked_1_, p_mouseClicked_3_, p_mouseClicked_5_);
		//this._groupPanel.mouseClicked(p_mouseClicked_1_, p_mouseClicked_3_, p_mouseClicked_5_);
		for(IOptionControl control : _optionPanel._optionControls) {
			GuiEventListener wid = control.getControl();
			if(wid.mouseClicked(p_mouseClicked_1_, p_mouseClicked_3_, p_mouseClicked_5_)) {
				this.setFocused(wid);
				if (p_mouseClicked_5_ == 0) {
					this.setDragging(true);
				}

				return true;
			}
		}
		/*if(_groupPanel.isInScrollArea((int)p_mouseClicked_1_, (int)p_mouseClicked_3_)) {
			int j = _groupPanel.getScrollPos();
			if(j > 0) {
				
			}
		}*/
		return false;
	}
	
	

	@Override
	public boolean mouseDragged(double p_mouseDragged_1_, double p_mouseDragged_3_, int p_mouseDragged_5_, double p_mouseDragged_6_, double p_mouseDragged_8_) {
		if(_optionPanel != null)
			_optionPanel.mouseDragged(p_mouseDragged_1_, p_mouseDragged_3_, p_mouseDragged_5_, p_mouseDragged_6_, p_mouseDragged_8_);
		return super.mouseDragged(p_mouseDragged_1_, p_mouseDragged_3_, p_mouseDragged_5_, p_mouseDragged_6_, p_mouseDragged_8_);
	}

	@Override
	public boolean mouseScrolled(double p_mouseScrolled_1_, double p_mouseScrolled_3_, double p_mouseScrolled_5_) {
		if(_optionPanel != null)
			_optionPanel.mouseScrolled(p_mouseScrolled_1_, p_mouseScrolled_3_, p_mouseScrolled_5_);
		return super.mouseScrolled(p_mouseScrolled_1_, p_mouseScrolled_3_, p_mouseScrolled_5_);
	}

	protected void actionPerformed(Button button)
	{
		if (button == this._doneButton)
		{
			super.minecraft.forceSetScreen(_parentGui);
		}
		else if (button == this._resetButton)
		{
			this.refreshGui = 3;
		}
	}
	
	@Override
	public void render(PoseStack pose, int mouseX, int mouseY, float partialTick) {
		this.renderBackground(pose);
		this._optionPanel.render(pose, mouseX, mouseY, partialTick);
		drawCenteredString(pose, font, "CustomOreGen Options", super.width / 2, tabNavigationBar.getRectangle().bottom() + 4, 16777215);
		super.render(pose, mouseX, mouseY, partialTick);
		
		if (this._toolTip != null) {
			this.renderTooltip(pose, Component.literal(_toolTip), mouseX, mouseY);
		}

		if (this.refreshGui > 0)
		{
			this.init();
		}
	}

	@Override
	public void tick() {
		this.tabManager.tickCurrent();
	}

	@Override
	public boolean keyPressed(int p_100875_, int p_100876_, int p_100877_) {
		if (this.tabNavigationBar.keyPressed(p_100875_)) {
			return true;
		} else if (super.keyPressed(p_100875_, p_100876_, p_100877_)) {
			return true;
		}
		return false;
	}
	
	private class DisplayGroupTab extends GridLayoutTab {
		public DisplayGroup group;
		
		public DisplayGroupTab(DisplayGroup option) {
			super(Component.literal(option.getLocalizedDisplayName()));
			group = option;
		}

		public void visitChildren(Consumer<AbstractWidget> p_268098_) {
			super.visitChildren(p_268098_);
			GuiCustomOreGenSettings.this.refreshGui = 1;
		}
	}

	public class GuiOptionSlot extends AbstractSelectionList<GuiOptionSlot.Entry> {
		protected final Screen _parentGui;
		protected final Vector<IOptionControl> _optionControls;
		protected IOptionControl _clickTarget;
		
		public GuiOptionSlot(Minecraft instance, int y0, int y1, int slotHeight, Vector<ConfigOption> options, GuiCustomOreGenSettings parentGui) {
			super(instance, GuiCustomOreGenSettings.this.width, GuiCustomOreGenSettings.this.height, y0, y1, slotHeight);
			this._parentGui = parentGui;
			this._optionControls = new Vector<IOptionControl>();
			this._clickTarget = null;
			
			for (int c = 0; c < options.size(); ++c)
			{
				ConfigOption option = options.get(c);

				if (option instanceof ChoiceOption)
				{
					this._optionControls.add(new GuiChoiceButton(2 * width / 5 + 15, 0, width / 10 + 100, slotHeight - 6, (ChoiceOption)option));
					this.addEntry(new Entry());
				}
				else if (option instanceof NumericOption)
				{
					this._optionControls.add(new GuiNumericSlider(2 * width / 5 + 15, 0, width / 10 + 100, slotHeight - 6, (NumericOption)option));
					this.addEntry(new Entry());
				}
			}
		}

		@Override
		protected int getItemCount() {
			return _optionControls.size();
		}

		@Override
		protected void renderItem(PoseStack pose, int mouseX, int mouseY, float partialTick, int index, int slotX, int slotY, int slotW, int slotH) {
			IOptionControl optCtrl = (IOptionControl)this._optionControls.get(index);
			ConfigOption option = optCtrl.getOption();
			AbstractWidget control = optCtrl.getControl();
			String optionName = option.getLocalizedDisplayName();
			int nameW = font.width(optionName);
			int nameX = 2 * width / 5 - 15 - nameW;
			int nameH = font.lineHeight;
			int nameY = slotY + 8;
			GuiComponent.drawString(pose, font, Component.literal(optionName), nameX, nameY, 16777215);

			if (mouseX >= nameX && mouseX <= nameX + nameW && mouseY >= nameY && mouseY <= nameY + nameH && this.isMouseOver(mouseX, mouseY))
			{
				_toolTip = option.getLocalizedDescription();
			}
			//TODO: fix this
			//if(control == null) return;
			control.setY(slotY + 3);
			control.render(pose, mouseX, mouseY, partialTick);
		}

		@Override
		public void updateNarration(NarrationElementOutput p_169152_) {
			
		}
		
		private static class Entry extends ObjectSelectionList.Entry<GuiOptionSlot.Entry> {
			@Override
			public void render(PoseStack p_93523_, int p_93524_, int p_93525_, int p_93526_, int p_93527_, int p_93528_, int p_93529_, int p_93530_, boolean p_93531_, float p_93532_) {
				
			}

			@Override
			public Component getNarration() {
				return null;
			}
		}
		
		class GuiNumericSlider extends AbstractSliderButton implements IOptionControl {
			private final NumericOption _numeric;

			public GuiNumericSlider(int x, int y, int w, int h, NumericOption option) {
				super(x, y, w, h, Component.literal(""), (double)option.getNormalizedDisplayValue());
				_numeric = option;
				applyValue();
			}

			@Override
			public ConfigOption getOption() {
				return _numeric;
			}

			@Override
			public AbstractWidget getControl() {
				return this;
			}

			@Override
			protected void updateMessage() {
				
			}

			@Override
			protected void applyValue() {
				this._numeric.setNormalizedDisplayValue(this.value);
				long prec = 6L;
				long base = (long)Math.pow(10.0D, (double)prec);

				for (long val = Math.round(this._numeric.getDisplayIncr() * (double)base); prec > 0L && val % 10L == 0L; --prec)
				{
					val /= 10L;
					base /= 10L;
				}
				
				super.setMessage(Component.literal(String.format("%." + prec + "f", new Object[] {Double.valueOf(this._numeric.getDisplayValue())})));
			}
			
		}
		
		class GuiChoiceButton extends AbstractButton implements IOptionControl {
			private final ChoiceOption _choice;
			private final int _maxWidth;
			
			public GuiChoiceButton(int x, int y, int w, int h, ChoiceOption choice) {
				super(x, y, w, h, Component.literal(choice.getLocalizedDisplayValue()));
				_choice = choice;
				_maxWidth = w;
				onValueChanged();
			}

			@Override
			public ConfigOption getOption() {
				return _choice;
			}

			@Override
			public AbstractWidget getControl() {
				return this;
			}

			@Override
			public void onPress() {
				this._choice.setValue(this._choice.nextPossibleValue());
				this.onValueChanged();
				refreshGui = this._choice.getDisplayState() == ConfigOption.DisplayState.shown_dynamic ? 2 : 1;
			}

			@Override
			protected void updateWidgetNarration(NarrationElementOutput p_259858_) {
				// TODO Auto-generated method stub
			}
			
			protected void onValueChanged()
			{
				super.setMessage(Component.literal(_choice.getLocalizedDisplayValue()));
				int strWidth = super.getMessage() == null ? 0 : font.width(super.getMessage());
				super.width = Math.min(this._maxWidth, strWidth + 10);
			}

		}
	}

	public interface IOptionControl
	{
		ConfigOption getOption();

		AbstractWidget getControl();
	}
}
