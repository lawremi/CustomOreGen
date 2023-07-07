package CustomOreGen.datagen;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import com.google.gson.JsonElement;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.HolderOwner;
import net.minecraft.core.HolderSet;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.common.data.JsonCodecProvider;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ForgeBiomeModifiers.RemoveFeaturesBiomeModifier;

public class COGFeatureProvider extends JsonCodecProvider<BiomeModifier> {
    private final Map<String, JsonElement> toSerialize = new HashMap<>();

	public COGFeatureProvider(PackOutput output, ExistingFileHelper existingFileHelper, String modid) {
		super(output, existingFileHelper, modid, JsonOps.INSTANCE, PackType.SERVER_DATA, "worldgen/placed_feature", RemoveFeaturesBiomeModifier.DIRECT_CODEC, Map.of());
	}

	@Override
	protected void gather(BiConsumer<ResourceLocation, BiomeModifier> consumer) {
		//HolderOwner owner;
		//add(new RemoveFeaturesBiomeModifier(new HolderSet.Named(owner, BiomeTags.IS_OVERWORLD), Set.of(), Set.of(Decoration.UNDERGROUND_ORES)));
	}

	public <T extends BiomeModifier> void add(String modifier, T instance)
    {
        JsonElement json = BiomeModifier.DIRECT_CODEC.encodeStart(JsonOps.INSTANCE, instance).getOrThrow(false, s -> {});
        this.toSerialize.put(modifier, json);
    }
}
