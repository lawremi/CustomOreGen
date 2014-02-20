package CustomOreGen.Config;

import CustomOreGen.Util.HeightScale;

public enum HeightScaleType {
	Base, Sealevel, Biome, Surface;
	
	public HeightScale getHeightScale() throws ParserException {
		String className = "CustomOreGen.Util." + name() + "HeightScale";
		try {
			Class<HeightScale> klass = (Class<HeightScale>) Class.forName(className);
			return klass.newInstance();
		} catch (Exception e) {
			throw new ParserException("Failed to construct HeightScale for type: " + name(), e);
		}
	}
}
