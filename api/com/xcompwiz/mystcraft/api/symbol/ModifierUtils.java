
package com.xcompwiz.mystcraft.api.symbol;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.world.biome.BiomeGenBase;

import com.xcompwiz.mystcraft.api.internals.BlockCategory;
import com.xcompwiz.mystcraft.api.internals.BlockDescriptor;
import com.xcompwiz.mystcraft.api.internals.Color;
import com.xcompwiz.mystcraft.api.internals.ColorGradient;

/**
 * A collection of helper functions for dealing with more complex modifier
 * types, such as blocks and biomes
 * Also includes a number of averaging functions to aid in consistent behavior
 * 
 * @author xcompwiz
 */
public final class ModifierUtils {

    public static float averageLengths(float l, float r) {
        return (l + r) / 2;
    }

    public static Color averageColors(Color left, Color right) {
        return left.average(right);
    }

    public static Color averageColors(Color color, float r, float g, float b) {
        return color.average(r, g, b);
    }

    /**
     * Returns a gradient from the current modifiers.
     * This will always return a gradient, though the gradient may be empty.
     * If there isn't a gradient object in the modifier system then a gradient
     * will be built
     * If the gradient is empty then the system will attempt to use an existing
     * color modifier
     * Note that if both are empty then the returned gradient will be empty
     * If the gradient modifier exists and is not empty, then any color
     * modifiers will be ignored
     * If a gradient modifier is set but is empty then the color modifier will
     * still be popped
     * 
     * @param controller
     *        The controller passed to the symbol during logic registration
     * @return A valid gradient object
     */
    public static ColorGradient popGradient(IAgeController controller) {
        ColorGradient gradient = controller.popModifier("gradient").asGradient();
        if (gradient == null) {
            gradient = new ColorGradient();
        }
        if (gradient.getColorCount() == 0) {
            gradient.pushColor(controller.popModifier("color").asColor());
        }
        return gradient;
    }

    /**
     * Returns a gradient from the current modifiers.
     * This will always return a gradient
     * If there isn't a gradient object in the modifier system then a gradient
     * will be built
     * If the gradient is empty then the system will attempt to use an existing
     * color modifier
     * If both modifiers are unset then the provided default color will be added
     * to the gradient
     * If the gradient modifier exists and is not empty, then any color
     * modifiers will be ignored
     * If a gradient modifier is set but is empty then the color modifier will
     * still be popped
     * 
     * @param controller
     *        The controller passed to the symbol during logic registration
     * @param r
     *        The default color value to use (red component)
     * @param g
     *        The default color value to use (green component)
     * @param b
     *        The default color value to use (blue component)
     * @return A valid gradient object
     */
    public static ColorGradient popGradient(IAgeController controller, float r, float g, float b) {
        ColorGradient gradient = popGradient(controller);
        if (gradient.getColorCount() == 0) {
            gradient.pushColor(new Color(r, g, b));
        }
        return gradient;
    }

    /**
     * Provides a block of a particular generation category if one is in the
     * queue
     * Will pop the first block satisfying the generation category found from
     * the top of the queue
     * 
     * @param controller
     *        The controller passed to the symbol during logic registration
     * @param type
     *        The generation category to attempt to retrieve
     * @return A block descriptor, if one satisfying the category is found.
     *         Otherwise null
     */
    public static BlockDescriptor popBlockOfType(IAgeController controller, BlockCategory type) {
        Modifier modifier = controller.popModifier("blocklist");
        List<BlockDescriptor> list = modifier.asList();
        if (list == null) {
            return null;
        }
        controller.setModifier("blocklist", modifier);
        for (int i = 0; i < list.size(); ++i) {
            BlockDescriptor block = list.get(i);
            if (block.isUsable(type)) {
                list.remove(i);
                modifier.dangling -= dangling_block;
                return block;
            }
        }
        return null;
    }

    /**
     * Adds a block descriptor to the top of the queue
     * 
     * @param controller
     *        The controller passed to the symbol during logic registration
     * @param block
     *        The block descriptor to push to the queue
     */
    public static void pushBlock(IAgeController controller, BlockDescriptor block) {
        Modifier modifier = controller.popModifier("blocklist");
        List<BlockDescriptor> list = modifier.asList();
        if (list == null) {
            list = new ArrayList<BlockDescriptor>();
            modifier = new Modifier(list, 0);
        }
        list.add(0, block);
        modifier.dangling += dangling_block;
        controller.setModifier("blocklist", modifier);
    }

    public static void pushBiome(IAgeController controller, BiomeGenBase biome) {
        Modifier modifier = controller.popModifier("biomelist");
        List<BiomeGenBase> list = modifier.asList();
        if (list == null) {
            list = new ArrayList<BiomeGenBase>();
            modifier = new Modifier(list, 0);
        }
        list.add(biome);
        modifier.dangling += dangling_biome;
        controller.setModifier("biomelist", modifier);
    }

    public static BiomeGenBase popBiome(IAgeController controller) {
        Modifier modifier = controller.popModifier("biomelist");
        List<BiomeGenBase> list = modifier.asList();
        if (list == null || list.size() == 0) {
            return null;
        }
        controller.setModifier("biomelist", modifier);
        BiomeGenBase biome = list.remove(list.size() - 1);
        modifier.dangling -= dangling_biome;
        return biome;
    }

    public static final int dangling_block = 50;
    public static final int dangling_biome = 100;
}
