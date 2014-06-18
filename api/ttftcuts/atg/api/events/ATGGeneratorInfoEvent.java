package ttftcuts.atg.api.events;

import java.util.List;

import net.minecraft.world.World;
import net.minecraftforge.event.Event;

public class ATGGeneratorInfoEvent extends Event {

	public World world;
	public double x;
	public double z;
	public double[] info;
	
	public ATGGeneratorInfoEvent(World world, double x, double z) {
		this.world = world;
		this.x = x;
		this.z = z;
	}
}