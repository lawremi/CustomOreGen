package CustomOreGen;

import CustomOreGen.Server.LevelGenFeature;
import CustomOreGen.Server.VanillaOreRemovalModifier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;

@EventBusSubscriber(modid = CustomOreGenBase.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ModEventBusSubscriber {
    @SubscribeEvent
	public static void onFMLPreInit(FMLCommonSetupEvent event) {
    	CustomPacketPayloadHandler.register();
    }
    
    /**
     * This Feature attempts to replicate what {@link ModEventBusSubscriber#generate} did previously.<br>
     *        Used to be called by registering this as an LevelAccessorGenerator.<br>
     *        This does two things: it first generates the distribution structures, without actually populating them.
     *        It then populates this chunk or any of its neighboring chunks if they now have a complete neighborhood.
     *        <br><br>
     *        The generation step could happen through the new Feature system, where each structure is a Feature.
     *        However, the population step falls outside of the system. There could be a populating Feature that runs after the rest.
     *        Better would be a Forge event when a chunk changes status. Population could run after FEATURES completion.
     */
    @SubscribeEvent
    public static void registerFeature(RegisterEvent event) {
    	if(event.getRegistryKey() == ForgeRegistries.Keys.FEATURES) {
			System.out.println("ModEventBusSubscriber registerFeature");
    		event.getForgeRegistry().register(
    				ResourceLocation.fromNamespaceAndPath(CustomOreGenBase.MODID, "cog_feature"),
    				new LevelGenFeature(NoneFeatureConfiguration.CODEC));
    	}
    	if(event.getRegistryKey() == ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS) {
    		event.getForgeRegistry().register(
    				ResourceLocation.fromNamespaceAndPath(CustomOreGenBase.MODID, "remove_vanilla_ores"),
    				VanillaOreRemovalModifier.CODEC);
    	}
    }
    
    @SubscribeEvent
	public static void onFMLPostInit(FMLLoadCompleteEvent event) {
    	CustomOreGenBase.onModPostLoad();
    }
}
