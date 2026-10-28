package CustomOreGen.Integration.MystCraft;


public class MystcraftObserver {
	private static MystcraftObserver _instance = new MystcraftObserver();
	
	private MystcraftObserver() {
		
	}
	
	public static MystcraftObserver instance() {
		return _instance;
	}
	/*
    @SubscribeEvent
    public void onDenseOres(DenseOresEvent event) {
    	ServerState.checkIfServerChanged(MinecraftServer.getServer(), event.levelObj.getLevelInfo());
    	WorldConfig config = ServerState.getWorldConfig(event.levelObj);
    	BlockDescriptor desc = config.getEquivalentBlockDescriptor();
    	LevelGenSubstitution substitution = new LevelGenSubstitution(config.getOreDistributions().size(), true);
    	BlockDescriptor subDesc = (BlockDescriptor) substitution.getDistributionSetting("OreBlock");
    	subDesc.clear();
    	subDesc.add(desc, 0.33F);
    	substitution.populate(event.levelObj, event.xPos, event.zPos);
    }
    */
}
