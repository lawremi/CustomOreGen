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
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.data.event.GatherDataEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = CustomOreGenBase.MODID)
public class COGDataGenerator {
	@SubscribeEvent
	public static void gatherData(GatherDataEvent.Server event) {
		DataGenerator generator = event.getGenerator();
		PackOutput packOutput = generator.getPackOutput();
		CompletableFuture<HolderLookup.Provider> lookupProvider = event.getWorldLookupProvider();
		RegistryAccess registryAccess = RegistryAccess.EMPTY;
		RegistryOps<JsonElement> registryOps = RegistryOps.create(JsonOps.INSTANCE, registryAccess);

		Identifier placedFeatureRL = Identifier.fromNamespaceAndPath(CustomOreGenBase.MODID, "ore_removal");
		//RemoveFeaturesBiomeModifier l;
		event.addProvider(new COGFeatureProvider(packOutput, lookupProvider, CustomOreGenBase.MODID));
	}
}
