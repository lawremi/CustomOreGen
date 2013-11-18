package com.xcompwiz.mystcraft.api.symbol.logic;

import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;

import com.xcompwiz.mystcraft.api.internals.IStorageObject;

public interface IWeatherController {
	public abstract void setDataObject(IStorageObject infoObj);
	public abstract void updateRaining();
	public abstract void tick(World worldObj, Chunk chunk);
	public abstract void clear();
	public abstract void togglePrecipitation();
	public abstract float getRainingStrength();
	public abstract float getStormStrength();
	public abstract float getTemperature(float current, int x, int z);
	public abstract boolean getEnableSnow(boolean current, int x, int y);
	public abstract boolean getEnableRain(boolean current, int x, int y);
}
