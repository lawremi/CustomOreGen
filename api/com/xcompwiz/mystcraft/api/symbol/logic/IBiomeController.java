package com.xcompwiz.mystcraft.api.symbol.logic;

import java.util.List;

import net.minecraft.world.biome.BiomeGenBase;

//FIXME: Stable?
public interface IBiomeController {
	public abstract List<BiomeGenBase> getValidSpawnBiomes();
	public abstract float[] getRainfallField(float af[], int x, int z, int x1, int z1);
	public abstract float[] getTemperatureField(float af[], int x, int z, int x1, int z1);

	public abstract BiomeGenBase getBiomeAtCoords(int i, int j);
	public abstract BiomeGenBase[] getBiomesAtCoords(BiomeGenBase abiomegenbase[], int i, int j, int k, int l, boolean flag);
	public abstract BiomeGenBase[] getBiomesFromGenerationField(BiomeGenBase[] abiomegenbase, int i, int j, int k, int l);
	public abstract void cleanupCache();
}