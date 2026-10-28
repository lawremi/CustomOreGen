package CustomOreGen;

import java.io.Serializable;

import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

public class MystcraftSymbolData implements Serializable
{
    public transient Level world;
    public Identifier dimensionID = null;
    public String symbolName;
    public int count = 0;
    public String displayName = "";
    public float weight = 1.0F;
    public float instability = 0.0F;
    private static final long serialVersionUID = 1L;

    public MystcraftSymbolData() {}

    public MystcraftSymbolData(Level world, String symbolName)
    {
        this.world = world;

        if (world != null)
        {
            this.dimensionID = world.dimension().identifier();
        }

        this.symbolName = this.displayName = symbolName;
    }
}
