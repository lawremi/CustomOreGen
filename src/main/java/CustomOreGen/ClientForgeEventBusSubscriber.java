package CustomOreGen;

import CustomOreGen.Client.ClientPacketHandler;
import CustomOreGen.Client.ClientState;
import CustomOreGen.Client.CreateWorldOptionsButton;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import net.minecraft.client.multiplayer.ClientLevel;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/**
 * Forge-bus events that only exist on the client. Forge 1.21 no longer allows @OnlyIn on subscriber
 * methods, so these live in a class that is only registered on the client.
 */
@EventBusSubscriber(modid = CustomOreGenBase.MODID, value = Dist.CLIENT)
public class ClientForgeEventBusSubscriber
{
    /** (A mod-bus event: subscribers get each event on the bus it's posted to.) */
    @SubscribeEvent
    public static void registerClientPayloadHandlers(RegisterClientPayloadHandlersEvent event)
    {
        event.register(CustomPacketPayloadHandler.Packet.TYPE, (packet, context) -> ClientPacketHandler.handle(packet.msg()));
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event)
    {
        Minecraft mc = Minecraft.getInstance();

        if (mc.level != null && ClientState.hasLevelChanged(mc.level))
        {
            ClientState.onLevelChanged(mc.level);
        }

        ClientState.onClientTick(mc);
    }

    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event)
    {
        ClientState.onRenderFrame(Minecraft.getInstance());
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
            CreateWorldOptionsButton.addOptionsButtonToGui((CreateWorldScreen)event.getScreen(), event.getListenersList(), event::addListener);
        }
    }

    /** Every frame, so the button follows the selected tab however it changes. */
    @SubscribeEvent
    public static void onScreenRender(ScreenEvent.Render.Pre event) {
        if (event.getScreen() instanceof CreateWorldScreen)
        {
            CreateWorldOptionsButton.updateOptionsButtonVisibility((CreateWorldScreen)event.getScreen());
        }
    }
}
