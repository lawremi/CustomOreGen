package CustomOreGen;

import java.io.Serializable;

import net.minecraft.world.World;

public class MystcraftSymbolData implements Serializable
{
    public transient World world;
    public int dimensionID = 0;
    public String symbolName;
    public int count = 0;
    public String displayName = "";
    public float weight = 1.0F;
    public float instability = 0.0F;
    private static final long serialVersionUID = 1L;

    public MystcraftSymbolData() {}

    public MystcraftSymbolData(World world, String symbolName)
    {
        this.world = world;

        if (world != null)
        {
            this.dimensionID = world.dimension.getType().getId();
        }

        this.symbolName = this.displayName = symbolName;
    }
}
