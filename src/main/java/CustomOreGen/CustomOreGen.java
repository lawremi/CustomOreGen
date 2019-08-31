package CustomOreGen;

import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;

@Mod(CustomOreGen.MODID)
public class CustomOreGen
{
    public static CustomOreGen instance;

    public static final String MODID = "customoregen";
    
    public CustomOreGen() {
    	instance = this;
    }
    
	private static ModContainer getModContainer() {
		return ModList.get().getModContainerByObject(instance).get();
	}

	public static String getDisplayString() {
		ModContainer metadata = CustomOreGen.getModContainer();
    	return metadata.getModId() + " " + metadata.getModInfo().getVersion();
	}
}
