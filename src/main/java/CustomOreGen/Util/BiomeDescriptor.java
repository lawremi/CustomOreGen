package CustomOreGen.Util;

import java.util.Collections;
import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.mojang.datafixers.util.Pair;

import CustomOreGen.Server.DistributionSettingMap.Copyable;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate.Parameter;
import net.minecraft.world.level.biome.Climate.ParameterPoint;
import net.minecraft.world.level.biome.MultiNoiseBiomeSourceParameterList;
import net.minecraft.core.Registry;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

public class BiomeDescriptor implements Copyable<BiomeDescriptor>
{
	protected LinkedList<Descriptor> _descriptors = new LinkedList<Descriptor>();
	protected Map<ResourceLocation,Float> _matches = new Hashtable<ResourceLocation, Float>();
	protected boolean _compiled = false;

	private String name;

	public BiomeDescriptor()
	{
		this.clear();
	}

	public BiomeDescriptor(String descriptor)
	{
		this.set(descriptor);
	}

	public void copyFrom(BiomeDescriptor source)
	{
		this._descriptors = new LinkedList<Descriptor>(source._descriptors);
		this._matches = new Hashtable<ResourceLocation,Float>(source._matches);
		this._compiled = source._compiled;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public BiomeDescriptor set(String descriptor)
	{
		this.clear();

		if (descriptor != null)
		{
			this.add(descriptor);
		}

		return this;
	}

	public BiomeDescriptor add(String descriptor)
	{
		return this.add(descriptor, 1.0F);
	}

	public BiomeDescriptor add(String descriptor, float weight)
	{
		return this.add(descriptor, 1.0F, new BiomeRestriction(), false);
	}

	public BiomeDescriptor add(String descriptor, float weight, BiomeRestriction climate, boolean describesType)
	{
		if (descriptor != null && weight != 0.0F)
		{
			this._compiled = false;
			this._descriptors.add(new Descriptor(descriptor, weight, climate, describesType));
		}

		return this;
	}

	public BiomeDescriptor addAll(BiomeDescriptor descriptor, float weight) {
		this._compiled = false;
		if (weight == 1.0F) {
			this._descriptors.addAll(descriptor._descriptors);
		} else {
			for (Descriptor desc : descriptor._descriptors) {
				add(desc.description, desc.weight * weight, desc.climate, desc.describesType);
			}
		}
		return this;
	}

	public BiomeDescriptor clear()
	{
		this._compiled = false;
		this._descriptors.clear();
		return this;
	}

	public List<Descriptor> getDescriptors()
	{
		return Collections.unmodifiableList(this._descriptors);
	}

	protected void add(Biome biome, float weight)
	{
		if (biome != null && weight != 0.0F)
		{
			Float currentValue = this._matches.get(biomeKey(biome));

			if (currentValue != null)
			{
				weight += currentValue.floatValue();
			}

			this._matches.put(biomeKey(biome), weight);
		}
	}

	protected float matchingWeight(Biome biome)
	{
		float totalWeight = 0.0F;

		ResourceLocation key = biomeKey(biome);
		String name = key == null ? null : key.toString();

		for (Descriptor desc : this._descriptors) {
			Matcher matcher;
			if (!desc.climate.isCompatible(biome))
				continue;

			if (desc.describesType) {
				if (hasBiomeType(biome, desc.description))
				{
					totalWeight += desc.weight;
				}
			} else {
				if (name != null)
				{
					// Patterns written before 1.13 name biomes without a namespace ("desert"), so also
					// try the path alone.
					matcher = desc.getPattern().matcher(name);

					if (matcher.matches() || desc.getPattern().matcher(key.getPath()).matches())
					{
						totalWeight += desc.weight;
					}
				}
			}
		}
		return totalWeight;
	}

	protected void compileMatches()
	{
		if (!this._compiled)
		{
			this._compiled = true;
			this._matches.clear();

			for (Biome biome : allBiomes()) {
				if (biome != null)
				{
					this.add(biome, this.matchingWeight(biome));
				}
			}
		}
	}

	public float getWeight(Biome biome)
	{
		this.compileMatches();
		Float value = this._matches.get(biomeKey(biome));
		return value == null ? 0.0F : value.floatValue();
	}

	public boolean matchesBiome(Biome biome, RandomSource rand)
	{
		float weight = this.getWeight(biome);

		if (weight <= 0.0F)
		{
			return false;
		}
		else if (weight < 1.0F)
		{
			if (rand == null)
			{
				rand = RandomSource.create();
			}

			return rand.nextFloat() < weight;
		}
		else
		{
			return true;
		}
	}

	public Biome getMatchingBiome(RandomSource rand)
	{
		this.compileMatches();
		float value = -1.0F;

		for (Entry<ResourceLocation,Float> entry : this._matches.entrySet()) {
			float weight = entry.getValue();
			Biome biome = biomeByKey(entry.getKey());

			if (weight > 0.0F)
			{
				if (weight >= 1.0F)
				{
					return biome;
				}

				if (value < 0.0F)
				{
					if (rand == null)
					{
						rand = RandomSource.create();
					}

					value = rand.nextFloat();
				}

				value -= weight;

				if (value < 0.0F)
				{
					return biome;
				}
			}

		}
		return null;
	}

	public float getTotalMatchWeight()
	{
		this.compileMatches();
		float weight = 0.0F;

		for (Float val : this._matches.values()) {
			if (val.floatValue() > 0.0F)
			{
				weight += val.floatValue();
			}
		}
		return weight;
	}

	public String toString()
	{
		switch (this._descriptors.size())
		{
		case 0:
			return "[no biomes]";

		case 1:
			return ((Descriptor)this._descriptors.get(0)).toString();

		default:
			return this._descriptors.toString();
		}
	}

	public String[] toDetailedString()
	{
		this.compileMatches();
		String[] breakdown = new String[this._matches.size() + 1];
		breakdown[0] = this._matches.size() + " biome matches";

		if (this._matches.size() > 0)
		{
			breakdown[0] = breakdown[0] + ':';
		}

		int i = 1;

		for (Entry<ResourceLocation,Float> entry : this._matches.entrySet()) {
			float weight = entry.getValue();
			Biome biome = biomeByKey(entry.getKey());

			if (biome == null)
			{
				breakdown[i] = "[??]";
			}
			else
			{
				breakdown[i] = biomeKey(biome).toString();
			}

			breakdown[i] = breakdown[i] + " - " + weight;
			++i;
		}

		return breakdown;
	}

	private static class Descriptor
	{
		public final String description;
		public final float weight;
		public final BiomeRestriction climate;
		public final boolean describesType;
		private Pattern pattern = null;

		public Descriptor(String description, float weight, BiomeRestriction climate, boolean describesType)
		{
			this.description = description;
			this.weight = weight;
			this.climate = climate;
			this.describesType = describesType;
		}

		public Pattern getPattern()
		{
			if (this.pattern == null)
			{
				this.pattern = Pattern.compile(this.description, Pattern.CASE_INSENSITIVE);
			}

			return this.pattern;
		}

		public String toString()
		{
			return this.description + " - " + Float.toString(this.weight);
		}
	}

	public static class BiomeRestriction {
		public final float minTemperature, maxTemperature;
		public final float minRainfall, maxRainfall;
		public final float minDepth, maxDepth;
		public final float minScale, maxScale;

		public BiomeRestriction(float minTemperature, float maxTemperature, float minRainfall, float maxRainfall,
				float minDepth, float maxDepth, float minScale, float maxScale) {
			this.minTemperature = minTemperature;
			this.maxTemperature = maxTemperature;
			this.minRainfall = minRainfall;
			this.maxRainfall = maxRainfall;
			this.minDepth = minDepth;
			this.maxDepth = maxDepth;
			this.minScale = minScale;
			this.maxScale = maxScale;
		}

		public BiomeRestriction() {
			this.minTemperature = this.minRainfall = this.minDepth = this.minScale = Float.NEGATIVE_INFINITY;
			this.maxTemperature = this.maxRainfall = this.maxDepth = this.maxScale = Float.POSITIVE_INFINITY;
		}

		public boolean isCompatible(Biome biome) {
			// Depth and scale come from the overworld climate table, which most restrictions don't use and
			// which has no entry for nether/end biomes; only consult it when the restriction needs it.
			return biome.getBaseTemperature() >= minTemperature && biome.getBaseTemperature() <= maxTemperature &&
					biome.getModifiedClimateSettings().downfall() >= minRainfall && biome.getModifiedClimateSettings().downfall() <= maxRainfall &&
					(unbounded(minDepth, maxDepth) || inRange(getDepth(biome), minDepth, maxDepth)) &&
					(unbounded(minScale, maxScale) || inRange(getScale(biome), minScale, maxScale));
		}

		private static boolean unbounded(float min, float max) {
			return min == Float.NEGATIVE_INFINITY && max == Float.POSITIVE_INFINITY;
		}

		// NaN (no climate entry for this biome) is never in range.
		private static boolean inRange(float value, float min, float max) {
			return value >= min && value <= max;
		}
	}

	private static Registry<Biome> _parametersFor = null;
	private static Map<Biome, ParameterPoint> _parameters = null;

	/**
	 * The biome's first cell in the overworld climate table, or null if it has none (nether, end, or
	 * modded biomes) or no server is running. Built once per server biome registry.
	 */
	public static synchronized ParameterPoint getParameters(Biome biome) {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server == null) {
			return null;
		}
		Registry<Biome> biomes = server.registryAccess().registryOrThrow(Registries.BIOME);
		if (biomes != _parametersFor) {
			HolderGetter<Biome> holdergetter = server.registryAccess().lookupOrThrow(Registries.BIOME);
			MultiNoiseBiomeSourceParameterList par = new MultiNoiseBiomeSourceParameterList(MultiNoiseBiomeSourceParameterList.Preset.OVERWORLD, holdergetter);
			Map<Biome, ParameterPoint> parameters = new IdentityHashMap<Biome, ParameterPoint>();
			for(Pair<ParameterPoint, Holder<Biome>> val : par.parameters().values()) {
				parameters.putIfAbsent(val.getSecond().value(), val.getFirst());
			}
			_parameters = parameters;
			_parametersFor = biomes;
		}
		return _parameters.get(biome);
	}

	public static float getDepth(Biome biome) {
		ParameterPoint p = getParameters(biome);
		if (p == null) return Float.NaN;
		Parameter depth = p.depth();
		return (depth.min() + depth.max()) / 2;
	}

	public static float getScale(Biome biome) {
		ParameterPoint p = getParameters(biome);
		if (p == null) return Float.NaN;
		Parameter cont = p.continentalness();
		Parameter eros = p.erosion();
		float contAvg = (cont.max() + cont.min()) / 2f;
		float erosAvg = 1 - ((eros.max() + eros.min()) / 2f);
		return (contAvg + erosAvg) / 2f;
	}

	// TODO: this is random BS math
	public static int getHeight(Biome biome, int sea, int rootHeight) {
		ParameterPoint p = getParameters(biome);
		if(p == null) return sea;
		Parameter depth = p.depth();
		Parameter cont = p.continentalness();
		Parameter eros = p.erosion();
		float contAvg = (cont.max() + cont.min()) / 2f;
		float erosAvg = 1 - ((eros.max() + eros.min()) / 2f);
		float depthAvg = (depth.max() + depth.min()) / 2f;
		return Mth.floor(((contAvg + erosAvg) / 2 * rootHeight) + (depthAvg * sea));
	}

	// Biomes are data-driven since 1.18: the Biome objects a running world hands out belong to the
	// server's biome registry, not ForgeRegistries.BIOMES, so keys must be looked up there.
	private static Registry<Biome> serverBiomes()
	{
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		return server == null ? null : server.registryAccess().registryOrThrow(Registries.BIOME);
	}

	// Biome Dictionary types (BiomeType in configs) became biome tags in 1.18: Forge's forge:is_<type>
	// for most, vanilla's minecraft:is_<type> for the rest. These two were renamed along the way.
	private static final Map<String, String> BIOME_TYPE_ALIASES = Map.of("mesa", "badlands", "hills", "hill");

	private static boolean hasBiomeType(Biome biome, String type)
	{
		Registry<Biome> biomes = serverBiomes();
		if (biomes == null)
		{
			return false;
		}
		Optional<Holder.Reference<Biome>> holder = biomes.getResourceKey(biome).flatMap(biomes::getHolder);
		if (holder.isEmpty())
		{
			return false;
		}
		String name = type.toLowerCase(Locale.ROOT);
		name = BIOME_TYPE_ALIASES.getOrDefault(name, name);
		for (String namespace : new String[] { "forge", "minecraft" })
		{
			ResourceLocation tag = ResourceLocation.tryBuild(namespace, "is_" + name);
			if (tag != null && holder.get().is(TagKey.create(Registries.BIOME, tag)))
			{
				return true;
			}
		}
		return false;
	}

	private static Iterable<Biome> allBiomes()
	{
		Registry<Biome> biomes = serverBiomes();
		return biomes != null ? biomes : ForgeRegistries.BIOMES;
	}

	private static ResourceLocation biomeKey(Biome biome)
	{
		Registry<Biome> biomes = serverBiomes();
		ResourceLocation key = biomes == null ? null : biomes.getKey(biome);
		return key != null ? key : ForgeRegistries.BIOMES.getKey(biome);
	}

	private static Biome biomeByKey(ResourceLocation key)
	{
		Registry<Biome> biomes = serverBiomes();
		Biome biome = biomes == null ? null : biomes.get(key);
		return biome != null ? biome : ForgeRegistries.BIOMES.getValue(key);
	}
}
