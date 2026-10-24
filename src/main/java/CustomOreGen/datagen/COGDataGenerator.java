package CustomOreGen.datagen;

import java.util.concurrent.CompletableFuture;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import CustomOreGen.CustomOreGenBase;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.data.event.GatherDataEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

@EventBusSubscriber(bus = EventBusSubscriber.Bus.MOD)
public class COGDataGenerator {
	@SubscribeEvent
	public static void gatherData(GatherDataEvent event) {
		DataGenerator generator = event.getGenerator();
		ExistingFileHelper fileHelper = event.getExistingFileHelper();
		PackOutput packOutput = generator.getPackOutput();
		CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
		RegistryAccess registryAccess = RegistryAccess.EMPTY;
		RegistryOps<JsonElement> registryOps = RegistryOps.create(JsonOps.INSTANCE, registryAccess);

		if(event.includeClient()) {

		}
		if(event.includeServer()) {
			ResourceLocation placedFeatureRL = ResourceLocation.fromNamespaceAndPath(CustomOreGenBase.MODID, "ore_removal");
			//RemoveFeaturesBiomeModifier l;
			generator.addProvider(event.includeServer(), new COGFeatureProvider(packOutput, fileHelper, CustomOreGenBase.MODID));
		}
	}
}
