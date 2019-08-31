package CustomOreGen;

import com.mojang.brigadier.CommandDispatcher;

import CustomOreGen.Client.ClientState;
import CustomOreGen.Server.ConsoleCommands;
import CustomOreGen.Server.ServerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.CreateWorldScreen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.CommandSource;
import net.minecraft.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.storage.SaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.Event.Result;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;

@EventBusSubscriber(modid = CustomOreGen.MODID)
public class ForgeEventBusSubscriber
{
    @SubscribeEvent
    public static void onServerStarting(FMLServerStartingEvent event)
    {
        ServerState.checkIfServerChanged(event.getServer(), (WorldInfo)null);    
        registerCommands(event.getCommandDispatcher());
    }

    private static void registerCommands(CommandDispatcher<CommandSource> dispatcher) {
    	CustomOreGenBase.log.debug("Registering Console command interface ...");
        ConsoleCommands.register(dispatcher);
	}

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent event)
    {
    	if (event.phase != TickEvent.Phase.END) {
    		return;
    	}
        Minecraft mc = Minecraft.getInstance();

        if (mc.world != null && ClientState.hasWorldChanged(mc.world))
        {
            ClientState.onWorldChanged(mc.world);
        }
    }

    @SubscribeEvent
    public static void onClientLogin(PlayerLoggedInEvent event)
    {
        World handlerWorld = event.getPlayer().world;
        ServerState.checkIfServerChanged(handlerWorld.getServer(), 
        		handlerWorld.getWorldInfo());
    }
    
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onRenderWorldLast(RenderWorldLastEvent event)
    {
        ClientState.onRenderWorld(Minecraft.getInstance().getRenderViewEntity(), (double)event.getPartialTicks());
    }

    @SubscribeEvent
    public static void onLoadWorld(WorldEvent.Load event)
    {
    	World world = event.getWorld().getWorld();
        if (world instanceof ServerWorld)
        {
            ServerState.checkIfServerChanged(world.getServer(), world.getWorldInfo());
            ServerState.getWorldConfig(world);
        }
        else if (event.getWorld() instanceof ClientWorld && ClientState.hasWorldChanged(world))
        {
            ClientState.onWorldChanged(world);
        }
    }

    @SubscribeEvent
    public static void onLoadChunk(ChunkEvent.Load event)
    {
    	// TODO: call populateDistributions, but instruct it to only generate if there is a version, and it's old
    }
    
    /*
     * FIXME: Remove the corresponding Feature objects from every biome.
     */
    @SubscribeEvent
    public static void onGenerateMinable(OreGenEvent.GenerateMinable event)
    {
    	World world = event.getWorld();
    	ServerState.checkIfServerChanged(world.getServer(), world.getWorldInfo());
        boolean vanillaOreGen = ServerState.getWorldConfig(world).vanillaOreGen;
        boolean isCustom = event.getType() == OreGenEvent.GenerateMinable.EventType.CUSTOM;
        boolean isOre;
        switch(event.getType()) {
		case COAL:
		case DIAMOND:
		case EMERALD:
		case GOLD:
		case IRON:
		case LAPIS:
		case QUARTZ:
		case REDSTONE:
			isOre = true;
			break;
		default:
			isOre = false;
        }
        event.setResult((vanillaOreGen || isCustom || !isOre) ? Result.ALLOW : Result.DENY);
    }
    
    @SubscribeEvent
    public static void onForceChunk(ForceChunkEvent event) {
    	ServerState.chunkForced(event.getTicket().world, event.getLocation());
    }
    
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() instanceof CreateWorldScreen)
        {
            ServerState.addOptionsButtonToGui((CreateWorldScreen)event.getGui(), event.getWidgetList());
        }
    }
    
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (event.getGui() instanceof CreateWorldScreen)
        {
            ServerState.updateOptionsButtonVisibility((CreateWorldScreen)event.getGui());
        }
    }
}
