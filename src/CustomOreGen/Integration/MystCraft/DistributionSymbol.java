package CustomOreGen.Integration.MystCraft;

import CustomOreGen.Server.IOreDistribution;

import com.xcompwiz.mystcraft.api.symbol.IAgeController;
import com.xcompwiz.mystcraft.api.symbol.IAgeSymbol;
import com.xcompwiz.mystcraft.api.symbol.logic.IPopulate;
import com.xcompwiz.mystcraft.api.symbol.words.WordData;

public class DistributionSymbol implements IAgeSymbol {

	private IOreDistribution distribution;
	private String suffix;
	
	@Override
	public void registerLogic(IAgeController controller, long seed) {
		/* Thoughts:
		 * We could build a config dynamically (at XML node level?)...
		 * and have an IPopulate that executes the config.
		 * That way, we could in theory serialize a Mystcraft script as XML.
		 * Also: we need a way to control the height. 
		 */
	}

	@Override
	public int instabilityModifier(int count) {
		return 0;
	}

	@Override
	public String identifier() {
		return "cog.dist." + distribution.getDistributionSetting(IOreDistribution.StandardSettings.Name.name());
	}

	@Override
	public String displayName() {
		return (String)distribution.getDistributionSetting(IOreDistribution.StandardSettings.DisplayName.name());
	}
	
	@Override
	public String[] getPoem() {
		return new String[] { WordData.Control, WordData.Machine, this.distribution.getNarayanWord(), this.suffix };
	}

}
