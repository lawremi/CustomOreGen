package com.xcompwiz.mystcraft.api.symbol.logic;

//FIXME: Stable?
public interface ILightingController {
	public abstract int scaleLighting(int blockLightValue);
	public abstract void generateLightBrightnessTable(float[] lightBrightnessTable);
}