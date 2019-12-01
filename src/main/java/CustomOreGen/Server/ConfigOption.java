package CustomOreGen.Server;

import CustomOreGen.Util.Localization;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;


public abstract class ConfigOption
{
    private final String _name;
    private String _displayName = null;
    private String _description = null;
    private DisplayState _displayState;
    private DisplayGroup _displayGroup;

    public ConfigOption(String name)
    {
        this._displayState = DisplayState.hidden;
        this._displayGroup = null;
        this._name = name;
    }

    public String getName()
    {
        return this._name;
    }

    public String getDisplayName()
    {
        return this._displayName == null ? this._name : this._displayName;
    }

    public void setDisplayName(String displayName)
    {
        this._displayName = displayName;
    }

    public String getDescription()
    {
        return this._description;
    }

    @OnlyIn(Dist.CLIENT)
    public String getLocalizedDescription() {
    	return this.localize("description", this.getDescription());
    }

    @OnlyIn(Dist.CLIENT)
    public String getLocalizedDisplayName() {
    	return this.localize("displayName", this.getDisplayName());
    }

    @OnlyIn(Dist.CLIENT)
	private String localize(String key, String value) {
		return Localization.maybeLocalize(this.getName() + "." + key, value); 
	}

    public void setDescription(String description)
    {
        this._description = description;
    }

    public void setDisplayState(DisplayState displayState)
    {
        this._displayState = displayState;
    }

    public DisplayState getDisplayState()
    {
        return this._displayState;
    }

    public void setDisplayGroup(DisplayGroup group)
    {
        this._displayGroup = group;
    }

    public DisplayGroup getDisplayGroup()
    {
        return this._displayGroup;
    }

    public abstract Object getValue();

    public abstract boolean setValue(Object var1);
    
    public static class DisplayGroup extends ConfigOption
    {
        public DisplayGroup(String name)
        {
            super(name);
        }

        public Object getValue()
        {
            return null;
        }

        public boolean setValue(Object value)
        {
            return false;
        }
    }


    public enum DisplayState
    {
    	hidden,
    	shown,
    	shown_dynamic;
    }

}
