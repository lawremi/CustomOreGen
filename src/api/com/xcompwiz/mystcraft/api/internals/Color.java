package com.xcompwiz.mystcraft.api.internals;

public final class Color {
	public final float r;
	public final float g;
	public final float b;

	public Color(float r, float g, float b) {
		this.r = r;
		this.g = g;
		this.b = b;
	}

	public Color(int color) {
		this.r = (color >> 16 & 255) / 255.0F;
		this.g = (color >>  8 & 255) / 255.0F;
		this.b = (color       & 255) / 255.0F;
	}

	public Color(java.awt.Color color) {
		this.r = color.getRed();
		this.g = color.getGreen();
		this.b = color.getBlue();
	}

	/**
	 * Averages this color with another color.
	 * Does not modify this object
	 * @param other The color to average with
	 * @return The averaged color
	 */
	public Color average(Color other) {
		Color color = new Color((r+other.r)/2,(g+other.g)/2,(b+other.b)/2);
		return color;
	}
	/**
	 * Averages this color with a set of values.
	 * Does not modify this object
	 * @param other The color to average with
	 * @return The averaged color
	 */
	public Color average(float red, float green, float blue) {
		Color color = new Color((r+red)/2,(g+green)/2,(b+blue)/2);
		return color;
	}

	/**
	 * Converts the color to an integer value
	 * @return The int representation of the color
	 */
	public int asInt() {
        int iColor = ((int)(this.r*255)<<16);
        iColor += ((int)(this.g*255)<<8);
        iColor += ((int)(this.b*255));
		return iColor;
	}

	/**
	 * Converts the color to a java awt color
	 * @return The color in AWT form
	 */
	public java.awt.Color toAWT() {
		return new java.awt.Color(this.r, this.g, this.b);
	}
}
