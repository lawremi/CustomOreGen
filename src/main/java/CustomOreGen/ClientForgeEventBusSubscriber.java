package CustomOreGen;

import CustomOreGen.Client.ClientState;
import CustomOreGen.Server.ServerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Forge-bus events that only exist on the client. Forge 1.21 no longer allows @OnlyIn on subscriber
 * methods, so these live in a class that is only registered on the client.
 */
@EventBusSubscriber(modid = CustomOreGenBase.MODID, value = Dist.CLIENT)
public class ClientForgeEventBusSubscriber
{
    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level != null && ClientState.hasLevelChanged(mc.level))
        {
            ClientState.onLevelChanged(mc.level);
        }
    }

    @SubscribeEvent
    public static void onRenderLevelLast(RenderLevelStageEvent event)
    {
        ClientState.onRenderLevel(event);
    }

    @SubscribeEvent
    public static void onLoadLevel(LevelEvent.Load event)
    {
        if (event.getLevel() instanceof ClientLevel cWorld && ClientState.hasLevelChanged(cWorld))
        {
            ClientState.onLevelChanged(cWorld);
        }
    }

    @SubscribeEvent
    public static void onInitGui(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof CreateWorldScreen)
        {
            ServerState.addOptionsButtonToGui((CreateWorldScreen)event.getScreen(), event.getListenersList(), event::addListener);
        }
    }

    @SubscribeEvent
    public static void onActionPerformed(ScreenEvent.MouseButtonReleased.Post event) {
        if (event.getScreen() instanceof CreateWorldScreen)
        {
            ServerState.updateOptionsButtonVisibility((CreateWorldScreen)event.getScreen());
        }
    }
}
