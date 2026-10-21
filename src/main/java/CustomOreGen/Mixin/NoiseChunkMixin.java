package CustomOreGen.Mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import CustomOreGen.Server.SaveLevelConfig;
import net.minecraft.world.level.levelgen.NoiseChunk;
import net.minecraft.world.level.levelgen.NoiseGeneratorSettings;

/**
 * Implements the vanillaOreVeins option. Minecraft's large ore veins (copper/granite, iron/tuff) are
 * filled in by terrain noise, which NoiseChunk only sets up when the dimension's noise settings say
 * ore_veins_enabled; this makes that check also respect the COG config. Patching the check, rather than
 * the settings themselves, leaves the world's saved settings untouched.
 */
@Mixin(NoiseChunk.class)
public class NoiseChunkMixin
{
	@Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/levelgen/NoiseGeneratorSettings;oreVeinsEnabled()Z"))
	private boolean customoregen$oreVeinsEnabled(NoiseGeneratorSettings settings)
	{
		return settings.oreVeinsEnabled() && SaveLevelConfig.vanillaOreVeins();
	}
}
