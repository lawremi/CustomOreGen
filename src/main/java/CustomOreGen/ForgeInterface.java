package CustomOreGen;

import java.io.File;

import CustomOreGen.Client.ClientState;
import CustomOreGen.Server.ServerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screen.CreateWorldScreen;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.World;
import net.minecraft.world.server.ServerChunkProvider;
import net.minecraft.world.server.ServerWorld;
import net.minecraft.world.storage.DimensionSavedDataManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.GuiScreenEvent;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.world.ChunkEvent;
import net.minecraftforge.event.world.WorldEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.ObfuscationReflectionHelper;

public class ForgeInterface
{
    public static ForgeInterface createAndRegister()
    {
        CustomOreGenBase.log.debug("Registering Forge interface ...");
        ForgeInterface inst = new ForgeInterface();
        MinecraftForge.EVENT_BUS.register(inst);
        //MinecraftForge.ORE_GEN_BUS.register(inst);
        //Enable when ready
        //MinecraftForge.EVENT_BUS.register(MystcraftObserver.instance());
        return inst;
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onRenderWorldLast(RenderWorldLastEvent event)
    {
        ClientState.onRenderWorld(Minecraft.getInstance().getRenderViewEntity(), (double)event.getPartialTicks());
    }

    @SubscribeEvent
    public void onLoadWorld(WorldEvent.Load event)
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
    public void onLoadChunk(ChunkEvent.Load event)
    {
    	// TODO: call populateDistributions, but instruct it to only generate if there is a version, and it's old
    }
    
    /*@SubscribeEvent
    public void onGenerateMinable(OreGenEvent.GenerateMinable event)
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
    }*/
    
    //TODO: replacement for ForceChunkEvent
    /*@SubscribeEvent
    public void onForceChunk(ForceChunkEvent event) {
    	ServerState.chunkForced(event.getTicket().world, event.getLocation());
    }*/
    
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onInitGui(GuiScreenEvent.InitGuiEvent.Post event) {
        if (event.getGui() instanceof CreateWorldScreen)
        {
            ServerState.addOptionsButtonToGui((CreateWorldScreen)event.getGui(), event.getWidgetList());
        }
    }
    
    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public void onActionPerformed(GuiScreenEvent.ActionPerformedEvent.Post event) {
        if (event.getGui() instanceof CreateWorldScreen)
        {
            ServerState.updateOptionsButtonVisibility((CreateWorldScreen)event.getGui());
        }
    }
    
    public static String getWorldDimensionFolder(World world)
    {
    	DimensionSavedDataManager instance = ((ServerChunkProvider)world.getChunkProvider()).getSavedData();
    	File file = ObfuscationReflectionHelper.getPrivateValue(DimensionSavedDataManager.class, instance , "folder");
    	return file.getName();
        //return world.provider.getSaveFolder();
    }
}
