package com.xcompwiz.mystcraft.api.symbol;

import java.util.Collection;
import java.util.List;
import java.util.Random;

import com.xcompwiz.mystcraft.api.internals.BlockCategory;

/**
 * Provides methods for interacting with the grammar rules for the symbol generation system
 * The implementation of this is provided by MystAPI
 * Do NOT implement this yourself!
 * @author xcompwiz
 */
public interface IGrammarAPI {

	/**
	 * Registers a rule for the grammar system
	 * In general, symbols providing critical logic should only have a rule for that element.  Otherwise, there is no limit to the number or kinds of rules that produce a symbol. 
	 * Example: {@code registerGrammarRule(IGrammarAPI.TERRAIN, 1.0F, IGrammarAPI.BLOCK_TERRAIN, "SymbolIdentifer")}
	 * Example: {@code registerGrammarRule(IGrammarAPI.TERRAINALT, 1.0F, IGrammarAPI.BLOCK_STRUCTURE, "SymbolIdentifer")}
	 * Rules must be registered before post-init
	 * @param parent The token to expand
	 * @param weight The weight value applied to the rule (higher -> more common).  1 is "Common"
	 * @param args The tokens to expand to
	 */
	void registerGrammarRule(String parent, float weight, String...args);

	/**
	 * Produces a list of symbols fro mthe given token
	 * @param token The token to expand
	 * @return A collection of all of the symbols which are contained in rules which directly expand the token
	 */
	Collection<IAgeSymbol> getSymbolsExpandingToken(String token);

	/**
	 * Produces a list of all the tokens which have rules that produce the provided token
	 * @param token The token produced
	 * @return A collection of all the parents of rules producing the given token
	 */
	Collection<String> getTokensProducingToken(String token);

	/**
	 * Produces a phrase from the given token
	 * @param token The token to use as the root of the phrase
	 * @param rand The random number generator to use during expansion
	 * @return The generated phrase as a list of terminals.  These should all be symbol identifiers.
	 */
	List<String> generateFromToken(String root, Random rand);

	/**
	 * Produces a phrase from the given token and a list of tokens to include, in order
	 * @param token The token to use as the root of the phrase
	 * @param rand The random number generator to use during parsing and expansion
	 * @param parsed A set of symbols to include, in order, parsed like when generating an age
	 * @return The generated phrase as a list of terminals.  These should all be symbol identifiers.
	 */
	List<String> generateFromToken(String root, Random rand, List<String> parsed);

	/** Generates a Biome */
	public static final String BIOME				= "Biome";
	/** Generates a number of  Biomes */
	public static final String BIOMES				= "Biomes";
	/** Generates a Biome Controller */
	public static final String BIOMECONTROLLER		= "BiomeController";
	/** Generates a Lighting Controller */
	public static final String LIGHTING				= "Lighting";
	/** Generates a Weather Controller */
	public static final String WEATHER				= "Weather";
	/** Generates a Base Terrain Generator */
	public static final String TERRAIN				= "TerrainGen";

	/** Generates a Visual Effect like Sky or Fog Color */
	public static final String VISUAL_EFFECT 		= "Visual";
	/** Generates a Terrain Alteration */
	public static final String TERRAINALT			= "TerrainAlt";
	/** Generates a Populator */
	public static final String POPULATOR			= "Populator";
	/** Generates a world Effect, like Accelerated */
	public static final String EFFECT				= "Effect";

	/** Generates a Sun */
	public static final String SUN					= "Sun";
	/** Generates a Moon */
	public static final String MOON					= "Moon";
	/** Generates a Starfield */
	public static final String STARFIELD			= "Starfield";
	/** Generates a DOODAD */
	public static final String DOODAD				= "Doodad";

	/** Generates a Sunset modifier about 20% of the time */
	public static final String SUNSET_UNCOMMON		= "SunsetUncommon";
	/** Generates a Sunset modifier */
	public static final String SUNSET				= "Sunset";

	/** Generates an Angle sequence */
	public static final String ANGLE				= "Angle";
	/** Generates a Period sequence */
	public static final String PERIOD				= "Period";
	/** Generates a Phase sequence */
	public static final String PHASE				= "Phase";
	/** Generates a Color sequence */
	public static final String COLOR				= "Color";
	/** Generates a Gradient sequence */
	public static final String GRADIENT				= "Gradient";

	/** Generates a Block Modifier which is a valid Terrain Block */
	public static final String BLOCK_TERRAIN		= BlockCategory.TERRAIN.getGrammarBinding();
	/** Generates a Block Modifier which is a valid Solid Block */
	public static final String BLOCK_SOLID			= BlockCategory.SOLID.getGrammarBinding();
	/** Generates a Block Modifier which is a valid Structure Block */
	public static final String BLOCK_STRUCTURE		= BlockCategory.STRUCTURE.getGrammarBinding();
	/** Generates a Block Modifier which is a valid Crystal Block */
	public static final String BLOCK_CRYSTAL		= BlockCategory.CRYSTAL.getGrammarBinding();
	/** Generates a Block Modifier which is a valid Sea Block */
	public static final String BLOCK_SEA			= BlockCategory.SEA.getGrammarBinding();
	/** Generates a Block Modifier which is a valid Fluid Block */
	public static final String BLOCK_FLUID			= BlockCategory.FLUID.getGrammarBinding();
	/** Generates a Block Modifier which is a valid Fluid Block */
	public static final String BLOCK_GAS			= BlockCategory.GAS.getGrammarBinding();
	/** Generates a Block Modifier */
	public static final String BLOCK_ANY			= BlockCategory.ANY.getGrammarBinding();
}
