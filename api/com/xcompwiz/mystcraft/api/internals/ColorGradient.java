
package com.xcompwiz.mystcraft.api.internals;

import java.util.ArrayList;
import java.util.List;

public final class ColorGradient {
    private List<Color> colors = new ArrayList<Color>();
    private List<Long> intervals = new ArrayList<Long>();
    private long length = 0;

    /**
     * Gets the number of color points in the gradient
     * 
     * @return The number of fixed colors in the gradient
     */
    public int getColorCount() {
        return this.colors.size();
    }

    /**
     * Returns the full length of the gradient
     * 
     * @return The length of the gradient
     */
    public long getLength() {
        return this.length;
    }

    /**
     * Adds a gradient to the end of this gradient
     * 
     * @param other
     *        The gradient to add to the end of this one
     */
    public void appendGradient(ColorGradient other) {
        for (int i = 0; i < other.colors.size(); ++i) {
            this.pushColor(other.colors.get(i), other.intervals.get(i));
        }
    }

    /**
     * Adds a color to the end of the gradient
     * 
     * @param color
     *        The color to add
     */
    public void pushColor(Color color) {
        this.pushColor(color, null);
    }

    /**
     * Adds a color to the gradient
     * 
     * @param color
     *        The color to add
     * @param interval
     *        How long it takes to transition to the next color
     */
    public void pushColor(Color color, Long interval) {
        if (color == null) {
            return;
        }
        if (interval == null || interval <= 0) {
            interval = 12000L;
        }
        this.colors.add(color);
        this.intervals.add((interval.longValue()));
        this.length += interval.longValue();
    }

    /**
     * Gets the current color at a point
     * 
     * @param time
     *        The point in the gradient to use
     * @return The color object that represents the current color
     */
    public Color getColor(long time) {
        //Case: No colors
        if (this.colors.size() == 0) {
            throw new RuntimeException("Whoops, empty gradient!");
        }

        //Case: Only one color
        if (this.colors.size() == 1) {
            return this.colors.get(0);
        }

        if (this.length <= 0) {
            return this.colors.get(0);
        }
        time = time % this.length;

        //Get first color
        int colorcounter = 0;
        while (time >= this.intervals.get(colorcounter)) {
            time -= this.intervals.get(colorcounter);
            colorcounter = (++colorcounter) % this.colors.size();
        }
        //Get second color
        int secondcolor = (colorcounter + 1) % this.colors.size();
        //Interpolate
        Color color1 = this.colors.get(colorcounter);
        Color color2 = this.colors.get(secondcolor);
        float interp = (float) time / (float) (this.intervals.get(colorcounter));
        Color colorout = new Color(this.interpolate(interp, color1.r, color2.r), this.interpolate(interp, color1.g, color2.g), this.interpolate(interp, color1.b, color2.b));
        return colorout;
    }

    private float interpolate(float interp, float val1, float val2) {
        return (val2 * interp) + (val1 * (1 - interp));
    }
}
