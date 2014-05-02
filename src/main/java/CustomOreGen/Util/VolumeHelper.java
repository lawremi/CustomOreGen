package CustomOreGen.Util;

public class VolumeHelper {
	public static double sphericalVolume(double r) {
		return ellipsoidalVolume(r, r, r);
	}
	
	public static double ellipsoidalVolume(double a, double b, double c) {
		return (4.0/3.0) * Math.PI * a * b * c;
	}

	public static double cylindricalVolume(double h, double r) {
		return h * Math.PI * r * r;
	}
}
