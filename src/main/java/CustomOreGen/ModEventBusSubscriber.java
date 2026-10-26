package CustomOreGen;

import CustomOreGen.Server.LevelGenFeature;
import CustomOreGen.Server.VanillaOreRemovalModifier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = CustomOreGenBase.MODID)
public class ModEventBusSubscriber {
    @SubscribeEvent
	public static void registerPayloads(RegisterPayloadHandlersEvent event) {
    	CustomPacketPayloadHandler.register(event);
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
    	event.register(Registries.FEATURE_TYPE, Identifier.fromNamespaceAndPath(CustomOreGenBase.MODID, "cog_feature"),
    			() -> LevelGenFeature.CODEC);
    	event.register(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, Identifier.fromNamespaceAndPath(CustomOreGenBase.MODID, "remove_vanilla_ores"),
    			() -> VanillaOreRemovalModifier.CODEC);
    }
    
    @SubscribeEvent
	public static void onFMLPostInit(FMLLoadCompleteEvent event) {
    	CustomOreGenBase.onModPostLoad();
    }
}
