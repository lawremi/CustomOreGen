package ttftcuts.atg.api.events;

import java.util.List;

import net.minecraftforge.event.Event;

public class ATGGeneratorInfoEvent extends Event {

	public double x;
	public double z;
	public List<Double> info;
	
	public ATGGeneratorInfoEvent(double x, double z) {
		this.x = x;
		this.z = z;
	}
}
