package CustomOreGen.Server;

import java.io.File;

import CustomOreGen.CustomOreGenBase;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * The options that apply to the whole server rather than one dimension, read once per server start
 * from a save-level WorldConfig (the global config plus this world's saved options). Used by things
 * that are set up before any level exists or that can't tell which level they are in: biome modifiers
 * and terrain noise. Per-level configs load from the same files, so they agree.
 */
public final class SaveLevelConfig
{
	private static volatile MinecraftServer _readFor = null;
	// Safe defaults if the config can't be read: leave Minecraft's own generation alone.
	private static volatile boolean _vanillaOreGen = true;
	private static volatile boolean _vanillaOreVeins = true;

	private SaveLevelConfig() {}

	/** Whether Minecraft's ore features (coal, iron, gold, ... blobs) generate. */
	public static boolean vanillaOreGen()
	{
		update();
		return _vanillaOreGen;
	}

	/** Whether Minecraft's noise-based large ore veins (copper/granite, iron/tuff) generate. */
	public static boolean vanillaOreVeins()
	{
		update();
		return _vanillaOreVeins;
	}

	// Called for every noise chunk, from several worldgen threads, so the common case takes no lock.
	private static void update()
	{
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server != null && server != _readFor)
		{
			read(server);
		}
	}

	private static synchronized void read(MinecraftServer server)
	{
		if (server == _readFor)
		{
			return;
		}
		_vanillaOreGen = true;
		_vanillaOreVeins = true;

		try
		{
			// Same folder the per-level configs use for save-level options (the overworld's data folder).
			// A brand-new world's data folder isn't created until the levels are, which is after biome
			// modifiers run, and WorldConfig saves the world's chosen options into it.
			File worldBaseDir = server.getWorldPath(LevelResource.ROOT).resolve("data").toFile();
			worldBaseDir.mkdirs();
			WorldConfig cfg = new WorldConfig(server.getWorldData(), worldBaseDir);
			_vanillaOreGen = cfg.vanillaOreGen;
			_vanillaOreVeins = cfg.vanillaOreVeins;
			CustomOreGenBase.log.info("Vanilla ore features: " + (_vanillaOreGen ? "on" : "off, removing them") + "; vanilla large ore veins: " + (_vanillaOreVeins ? "on" : "off"));
		}
		catch (Exception e)
		{
			CustomOreGenBase.log.error("Could not read the save-level COG config; leaving vanilla ore generation in place.", e);
		}
		// Published last, so the fast path never sees this server with the previous server's values.
		_readFor = server;
	}
}
