package com.mutualzz.voice;

import com.mojang.blaze3d.platform.InputConstants;
import io.netty.buffer.Unpooled;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.ClientRegistry;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.event.EventNetworkChannel;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;

@Mod(VoiceClientController.MOD_ID)
public final class MutualzzVoiceForge {
    private static final ResourceLocation CHANNEL_ID = new ResourceLocation("mutualzz", "voice");
    private static final String PROTOCOL = "1";
    private static final VoiceClientController.Keys KEYS = new VoiceClientController.Keys();

    private static EventNetworkChannel channel;

    public MutualzzVoiceForge() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        channel = NetworkRegistry.newEventChannel(
                CHANNEL_ID,
                () -> PROTOCOL,
                v -> true,
                v -> true
        );
        channel.addListener(MutualzzVoiceForge::onChannelPayload);

        if (!FMLEnvironment.dist.isClient()) {
            return;
        }

        VoiceSettings.get().load();
        modBus.addListener(this::onClientSetup);
        MinecraftForge.EVENT_BUS.register(this);

        VoiceNetworking.setBackend(MutualzzVoiceForge::sendJson);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        KEYS.mute = mapping("key.mutualzz_voice.mute", GLFW.GLFW_KEY_M, KeyModifier.ALT);
        KEYS.deafen = mapping("key.mutualzz_voice.deafen", GLFW.GLFW_KEY_D, KeyModifier.SHIFT);
        KEYS.speakingHud = mapping("key.mutualzz_voice.speaking_hud", GLFW.GLFW_KEY_H, KeyModifier.ALT);
        KEYS.ptt = new KeyMapping(
                "key.mutualzz_voice.ptt",
                KeyConflictContext.IN_GAME,
                KeyModifier.NONE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                VoiceClientController.CATEGORY
        );
        KEYS.inputMode = mapping("key.mutualzz_voice.input_mode", GLFW.GLFW_KEY_I, KeyModifier.ALT);
        KEYS.volumeUp = mapping("key.mutualzz_voice.volume_up", GLFW.GLFW_KEY_EQUAL, KeyModifier.ALT);
        KEYS.volumeDown = mapping("key.mutualzz_voice.volume_down", GLFW.GLFW_KEY_MINUS, KeyModifier.ALT);
        KEYS.micSensUp = mapping("key.mutualzz_voice.mic_sens_up", GLFW.GLFW_KEY_EQUAL, KeyModifier.SHIFT);
        KEYS.micSensDown = mapping("key.mutualzz_voice.mic_sens_down", GLFW.GLFW_KEY_MINUS, KeyModifier.SHIFT);
        KEYS.selectNext = mapping("key.mutualzz_voice.select_next", GLFW.GLFW_KEY_RIGHT_BRACKET, KeyModifier.ALT);
        KEYS.selectPrev = mapping("key.mutualzz_voice.select_prev", GLFW.GLFW_KEY_LEFT_BRACKET, KeyModifier.ALT);
        KEYS.userVolumeUp = mapping("key.mutualzz_voice.user_volume_up", GLFW.GLFW_KEY_UP, KeyModifier.ALT);
        KEYS.userVolumeDown = mapping("key.mutualzz_voice.user_volume_down", GLFW.GLFW_KEY_DOWN, KeyModifier.ALT);
        KEYS.userMute = mapping("key.mutualzz_voice.user_mute", GLFW.GLFW_KEY_U, KeyModifier.ALT);
        KEYS.settings = mapping("key.mutualzz_voice.settings", GLFW.GLFW_KEY_O, KeyModifier.ALT);

        ClientRegistry.registerKeyBinding(KEYS.mute);
        ClientRegistry.registerKeyBinding(KEYS.deafen);
        ClientRegistry.registerKeyBinding(KEYS.speakingHud);
        ClientRegistry.registerKeyBinding(KEYS.ptt);
        ClientRegistry.registerKeyBinding(KEYS.inputMode);
        ClientRegistry.registerKeyBinding(KEYS.volumeUp);
        ClientRegistry.registerKeyBinding(KEYS.volumeDown);
        ClientRegistry.registerKeyBinding(KEYS.micSensUp);
        ClientRegistry.registerKeyBinding(KEYS.micSensDown);
        ClientRegistry.registerKeyBinding(KEYS.selectNext);
        ClientRegistry.registerKeyBinding(KEYS.selectPrev);
        ClientRegistry.registerKeyBinding(KEYS.userVolumeUp);
        ClientRegistry.registerKeyBinding(KEYS.userVolumeDown);
        ClientRegistry.registerKeyBinding(KEYS.userMute);
        ClientRegistry.registerKeyBinding(KEYS.settings);
    }

    private static void onChannelPayload(final NetworkEvent.ClientCustomPayloadEvent event) {
        FriendlyByteBuf payload = event.getPayload();
        NetworkEvent.Context ctx = event.getSource().get();
        if (payload == null) {
            return;
        }
        byte[] data = new byte[payload.readableBytes()];
        payload.readBytes(data);
        String json = new String(data, StandardCharsets.UTF_8);
        ctx.enqueueWork(() -> VoiceSession.handlePluginMessage(json));
        ctx.setPacketHandled(true);
    }

    private static void sendJson(String json) {
        Minecraft client = Minecraft.getInstance();
        if (client.getConnection() == null) {
            return;
        }
        FriendlyByteBuf buf = new FriendlyByteBuf(Unpooled.buffer());
        buf.writeBytes(json.getBytes(StandardCharsets.UTF_8));
        client.getConnection().send(new ServerboundCustomPayloadPacket(CHANNEL_ID, buf));
    }

    private static KeyMapping mapping(String name, int key, KeyModifier modifier) {
        return new KeyMapping(
                name,
                KeyConflictContext.IN_GAME,
                modifier,
                InputConstants.Type.KEYSYM,
                key,
                VoiceClientController.CATEGORY
        );
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        VoiceClientController.onClientTick(Minecraft.getInstance(), KEYS);
    }

    @SubscribeEvent
    public void onRenderGui(RenderGameOverlayEvent.Post event) {
        if (event.getType() != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }
        var font = Minecraft.getInstance().font;
        var pose = event.getMatrixStack();
        VoiceClientController.renderHud((text, x, y, color, shadow) -> {
            if (shadow) {
                font.drawShadow(pose, text, x, y, color);
            } else {
                font.draw(pose, text, x, y, color);
            }
        });
    }

    @SubscribeEvent
    public void onLogout(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        VoiceSession.leave();
    }
}
