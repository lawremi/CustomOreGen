package com.xcompwiz.mystcraft.api.symbol.logic;

import com.xcompwiz.mystcraft.api.internals.Color;

//FIXME: Stable?
public interface ICloudColorProvider {
	public abstract Color getCloudColor(float time, float celestial_angle);
}