package CustomOreGen.Integration.MystCraft;

import java.util.logging.Level;

import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.ForgeSubscribe;
import CustomOreGen.CustomOreGenBase;
import CustomOreGen.Server.ServerState;
import CustomOreGen.Server.WorldConfig;
import CustomOreGen.Server.WorldGenSubstitution;
import CustomOreGen.Util.BlockDescriptor;

import com.xcompwiz.mystcraft.api.event.DenseOresEvent;

public class MystcraftObserver {
	private static MystcraftObserver _instance = new MystcraftObserver();
	
	private MystcraftObserver() {
		
	}
	
	public static MystcraftObserver instance() {
		return _instance;
	}
	
    @ForgeSubscribe
    public void onDenseOres(DenseOresEvent event) {
    	ServerState.checkIfServerChanged(MinecraftServer.getServer(), event.worldObj.getWorldInfo());
    	WorldConfig config = ServerState.getWorldConfig(event.worldObj);
    	BlockDescriptor desc = config.getEquivalentBlockDescriptor();
    	WorldGenSubstitution substitution = new WorldGenSubstitution(config.getOreDistributions().size(), true);
    	BlockDescriptor subDesc = (BlockDescriptor) substitution.getDistributionSetting("OreBlock");
    	subDesc.clear();
    	subDesc.add(desc, 0.33F);
    	substitution.populate(event.worldObj, event.xPos, event.zPos);
    }
}
