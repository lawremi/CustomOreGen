package CustomOreGen;

import CustomOreGen.Server.ConsoleCommands;
import CustomOreGen.Server.ServerState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;

/** Forge-bus events on both sides; the client-only ones are in ClientForgeEventBusSubscriber. */
@EventBusSubscriber(modid = CustomOreGenBase.MODID)
public class ForgeEventBusSubscriber
{
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event)
    {
    	CustomOreGenBase.log.debug("Registering Console command interface ...");
        ConsoleCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onClientLogin(PlayerLoggedInEvent event)
    {
        Level handlerLevel = event.getEntity().level();
        ServerState.checkIfServerChanged(handlerLevel.getServer(), 
        		handlerLevel.getServer().getWorldData());
    }
    
    @SubscribeEvent
    public static void onLoadLevel(LevelEvent.Load event)
    {
    	LevelAccessor world = event.getLevel();
        if (world instanceof ServerLevel sWorld)
        {
            ServerState.checkIfServerChanged(sWorld.getServer(), sWorld.getServer().getWorldData());
            ServerState.getWorldConfig(sWorld);
        }
    }

    @SubscribeEvent
    public static void onLoadChunk(ChunkEvent.Load event)
    {
    	// TODO: call populateDistributions, but instruct it to only generate if there is a version, and it's old
    }
}
