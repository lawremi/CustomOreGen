package CustomOreGen;

import java.util.Iterator;

import net.minecraft.block.Block;
import net.minecraft.tags.Tag;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStage.Decoration;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.DecoratedFeatureConfig;
import net.minecraft.world.gen.feature.OreFeatureConfig;
import net.minecraft.world.gen.feature.ReplaceBlockConfig;
import net.minecraftforge.common.Tags;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.event.lifecycle.FMLLoadCompleteEvent;
import net.minecraftforge.registries.ForgeRegistries;

@EventBusSubscriber(modid = CustomOreGen.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ForgeModBusSubscriber
{
    @SubscribeEvent
    public static void onServerStarting(FMLLoadCompleteEvent event)
    {
    	//Two ways to go about removing generators: by specific block (in this case all ores)
    	/*for(Block b : Tags.Blocks.ORES.getAllElements()) {
			removeGenerator(b);
		}*/
    	//Or remove everything that is in a given tag
    	removeAllGenerators(Tags.Blocks.ORES);
    }
    
	private static void removeAllGenerators(Tag<Block> tag) {
		for(Biome biome : ForgeRegistries.BIOMES) { //for each biome
			Iterator<ConfiguredFeature<?>> it = biome.getFeatures(Decoration.UNDERGROUND_ORES).iterator();
			while(it.hasNext()) { //for each feature in that biome
				ConfiguredFeature<?> feature = it.next();
				if(feature.config instanceof DecoratedFeatureConfig) {
					DecoratedFeatureConfig dfconfig = (DecoratedFeatureConfig)feature.config;
					if(dfconfig.feature.config instanceof OreFeatureConfig) { //if it is an ore gen feature
						OreFeatureConfig oreConfigl = (OreFeatureConfig)dfconfig.feature.config;
						if(tag.contains(oreConfigl.state.getBlock())) { //and matches any ore block
							it.remove(); //remove it
						}
					}
					if(dfconfig.feature.config instanceof ReplaceBlockConfig) { //emerald ore uses this
						ReplaceBlockConfig oreConfigl = (ReplaceBlockConfig)dfconfig.feature.config;
						if(tag.contains(oreConfigl.state.getBlock())) { //and matches any ore block
							it.remove(); //remove it
						}
					}
					//there are other implementors of IFeatureConfig
					//such as clay deposits (SphereReplaceConfig) that may also be of interest
				}
			}
		}
	}

	private static void removeGenerator(Block vanillaOre) {
		for(Biome biome : ForgeRegistries.BIOMES) { //for each biome
			Iterator<ConfiguredFeature<?>> it = biome.getFeatures(Decoration.UNDERGROUND_ORES).iterator();
			while(it.hasNext()) { //for each feature in biome
				ConfiguredFeature<?> feature = it.next();
				if(feature.config instanceof DecoratedFeatureConfig) {
					DecoratedFeatureConfig dfconfig = (DecoratedFeatureConfig)feature.config;
					if(dfconfig.feature.config instanceof OreFeatureConfig) { //if it is an ore gen feature
						OreFeatureConfig oreConfigl = (OreFeatureConfig)dfconfig.feature.config;
						if(oreConfigl.state.getBlock() == vanillaOre) { //and matches desired block
							it.remove(); //remove it
						}
					}
					if(dfconfig.feature.config instanceof ReplaceBlockConfig) { //emerald ore uses this
						ReplaceBlockConfig oreConfigl = (ReplaceBlockConfig)dfconfig.feature.config;
						if(oreConfigl.state.getBlock() == vanillaOre) { //and matches desired block
							it.remove(); //remove it
						}
					}
				}
			}
		}
	}
}
