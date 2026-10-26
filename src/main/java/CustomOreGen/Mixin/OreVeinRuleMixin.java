package CustomOreGen.Mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import CustomOreGen.Server.SaveLevelConfig;
import net.minecraft.world.level.levelgen.material.MaterialRuleContext;
import net.minecraft.world.level.levelgen.material.rule.OreVeinRule;
import net.minecraft.world.level.levelgen.material.rule.RuleEvaluator;

/**
 * Implements the vanillaOreVeins option. Minecraft's large ore veins (copper/granite, iron/tuff) are a material
 * rule of the overworld's terrain; with the option off, the rule places nothing (as vanilla's debug switch does).
 * Patching the rule, rather than the world's settings, leaves the saved settings untouched.
 */
@Mixin(OreVeinRule.class)
public class OreVeinRuleMixin
{
	@Inject(method = "compile", at = @At("HEAD"), cancellable = true)
	private void customoregen$noOreVeins(MaterialRuleContext context, CallbackInfoReturnable<RuleEvaluator> cir)
	{
		if (!SaveLevelConfig.vanillaOreVeins())
		{
			cir.setReturnValue((x, y, z) -> null);
		}
	}
}
