package CustomOreGen.Server;

import java.util.List;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep.Decoration;
import net.minecraft.world.level.levelgen.feature.configurations.OreConfiguration;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;

/**
 * customoregen:remove_vanilla_ores. Implements the config's vanillaOreGen option ("Allow Vanilla Ore
 * Generation?"): when it is off, removes vanilla (minecraft:) ore features from the listed biomes, so
 * COG's distributions are the only source of ore there. Before 1.13 this was done by denying Forge's
 * ore generation events, which no longer exist.
 *
 * Biome modifiers run once at server start, before any level exists, and apply to the whole server,
 * so the option comes from SaveLevelConfig rather than a per-dimension config.
 */
public record VanillaOreRemovalModifier(HolderSet<Biome> biomes) implements BiomeModifier
{
	public static final MapCodec<VanillaOreRemovalModifier> CODEC = RecordCodecBuilder.mapCodec(builder -> builder.group(
			Biome.LIST_CODEC.fieldOf("biomes").forGetter(VanillaOreRemovalModifier::biomes)
		).apply(builder, VanillaOreRemovalModifier::new));

	// Vanilla puts overworld ores in UNDERGROUND_ORES and nether ores in UNDERGROUND_DECORATION.
	private static final Decoration[] ORE_STEPS = { Decoration.UNDERGROUND_ORES, Decoration.UNDERGROUND_DECORATION };

	@Override
	public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder)
	{
		if (phase != Phase.REMOVE || !this.biomes.contains(biome) || SaveLevelConfig.vanillaOreGen())
		{
			return;
		}

		for (Decoration step : ORE_STEPS)
		{
			List<Holder<PlacedFeature>> features = builder.getGenerationSettings().getFeatures(step);
			features.removeIf(VanillaOreRemovalModifier::isVanillaOreFeature);
		}
	}

	@Override
	public MapCodec<? extends BiomeModifier> codec()
	{
		return CODEC;
	}

	/**
	 * A minecraft: placed feature that places ore (an ore or scattered_ore feature whose targets include a
	 * block tagged c:ores). Dirt, gravel, granite, tuff, etc. blobs use the same feature type but
	 * aren't ores, so they stay.
	 */
	private static boolean isVanillaOreFeature(Holder<PlacedFeature> placed)
	{
		boolean vanilla = placed.unwrapKey().map(key -> key.location().getNamespace().equals("minecraft")).orElse(false);
		if (!vanilla)
		{
			return false;
		}
		if (placed.value().feature().value().config() instanceof OreConfiguration ore)
		{
			return ore.targetStates.stream().anyMatch(target -> target.state.is(Tags.Blocks.ORES));
		}
		return false;
	}
}
