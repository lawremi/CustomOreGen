package CustomOreGen.datagen;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.data.JsonCodecProvider;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.BiomeModifiers.RemoveFeaturesBiomeModifier;

public class COGFeatureProvider extends JsonCodecProvider<BiomeModifier> {
    private final Map<String, JsonElement> toSerialize = new HashMap<>();

	public COGFeatureProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> registries, ExistingFileHelper existingFileHelper, String modid) {
		super(output, PackOutput.Target.DATA_PACK, "worldgen/placed_feature", PackType.SERVER_DATA, BiomeModifier.DIRECT_CODEC, registries, modid, existingFileHelper);
	}

	@Override
	protected void gather() {
		//HolderOwner owner;
		//add(new RemoveFeaturesBiomeModifier(new HolderSet.Named(owner, BiomeTags.IS_OVERWORLD), Set.of(), Set.of(Decoration.UNDERGROUND_ORES)));
	}

	public <T extends BiomeModifier> void add(String modifier, T instance)
    {
        JsonElement json = BiomeModifier.DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, instance).getOrThrow();
        this.toSerialize.put(modifier, json);
    }
}
