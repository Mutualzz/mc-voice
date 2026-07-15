package com.mutualzz.voice;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TickEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlerEvent;
import net.neoforged.neoforge.network.handling.PlayPayloadContext;
import org.lwjgl.glfw.GLFW;

@Mod(VoiceClientController.MOD_ID)
public final class MutualzzVoiceNeoForge {
    private static final VoiceClientController.Keys KEYS = new VoiceClientController.Keys();

    public MutualzzVoiceNeoForge() {
        if (FMLEnvironment.dist != Dist.CLIENT) {
            return;
        }

        VoiceSettings.get().load();

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::registerPayloads);
        modBus.addListener(this::registerKeys);

        NeoForge.EVENT_BUS.addListener(this::onClientTick);
        NeoForge.EVENT_BUS.addListener(this::onRenderGui);
        NeoForge.EVENT_BUS.addListener(this::onLogout);

        VoiceNetworking.setBackend(json -> {
            var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
            if (connection != null) {
                connection.send(new net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket(
                        new VoicePayload(json)
                ));
            }
        });
    }

    private void registerPayloads(RegisterPayloadHandlerEvent event) {
        event.registrar("mutualzz")
                .optional()
                .play(VoicePayload.ID, VoicePayload::new, handler ->
                        handler.client(MutualzzVoiceNeoForge::handlePayload));
    }

    private static void handlePayload(VoicePayload payload, PlayPayloadContext context) {
        context.workHandler().execute(() -> VoiceSession.handlePluginMessage(payload.json()));
    }

    private void registerKeys(RegisterKeyMappingsEvent event) {
        KEYS.mute = mapping("key.mutualzz_voice.mute", GLFW.GLFW_KEY_M, KeyModifier.CONTROL);
        KEYS.deafen = mapping("key.mutualzz_voice.deafen", GLFW.GLFW_KEY_D, KeyModifier.ALT);
        KEYS.speakingHud = mapping("key.mutualzz_voice.speaking_hud", GLFW.GLFW_KEY_H, KeyModifier.CONTROL);
        KEYS.ptt = new KeyMapping(
                "key.mutualzz_voice.ptt",
                KeyConflictContext.IN_GAME,
                KeyModifier.NONE,
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                VoiceClientController.CATEGORY
        );
        KEYS.inputMode = mapping("key.mutualzz_voice.input_mode", GLFW.GLFW_KEY_I, KeyModifier.CONTROL);
        KEYS.volumeUp = mapping("key.mutualzz_voice.volume_up", GLFW.GLFW_KEY_EQUAL, KeyModifier.CONTROL);
        KEYS.volumeDown = mapping("key.mutualzz_voice.volume_down", GLFW.GLFW_KEY_MINUS, KeyModifier.CONTROL);
        KEYS.micSensUp = mapping("key.mutualzz_voice.mic_sens_up", GLFW.GLFW_KEY_EQUAL, KeyModifier.ALT);
        KEYS.micSensDown = mapping("key.mutualzz_voice.mic_sens_down", GLFW.GLFW_KEY_MINUS, KeyModifier.ALT);
        KEYS.selectNext = mapping("key.mutualzz_voice.select_next", GLFW.GLFW_KEY_RIGHT_BRACKET, KeyModifier.CONTROL);
        KEYS.selectPrev = mapping("key.mutualzz_voice.select_prev", GLFW.GLFW_KEY_LEFT_BRACKET, KeyModifier.CONTROL);
        KEYS.userVolumeUp = mapping("key.mutualzz_voice.user_volume_up", GLFW.GLFW_KEY_UP, KeyModifier.CONTROL);
        KEYS.userVolumeDown = mapping("key.mutualzz_voice.user_volume_down", GLFW.GLFW_KEY_DOWN, KeyModifier.CONTROL);
        KEYS.userMute = mapping("key.mutualzz_voice.user_mute", GLFW.GLFW_KEY_U, KeyModifier.CONTROL);
        KEYS.settings = mapping("key.mutualzz_voice.settings", GLFW.GLFW_KEY_O, KeyModifier.CONTROL);

        event.register(KEYS.mute);
        event.register(KEYS.deafen);
        event.register(KEYS.speakingHud);
        event.register(KEYS.ptt);
        event.register(KEYS.inputMode);
        event.register(KEYS.volumeUp);
        event.register(KEYS.volumeDown);
        event.register(KEYS.micSensUp);
        event.register(KEYS.micSensDown);
        event.register(KEYS.selectNext);
        event.register(KEYS.selectPrev);
        event.register(KEYS.userVolumeUp);
        event.register(KEYS.userVolumeDown);
        event.register(KEYS.userMute);
        event.register(KEYS.settings);
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

    private void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        VoiceClientController.onClientTick(net.minecraft.client.Minecraft.getInstance(), KEYS);
    }

    private void onRenderGui(RenderGuiEvent.Post event) {
        var font = net.minecraft.client.Minecraft.getInstance().font;
        var graphics = event.getGuiGraphics();
        VoiceClientController.renderHud((text, x, y, color, shadow) ->
                graphics.drawString(font, text, x, y, color, shadow));
    }

    private void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        VoiceSession.leave();
    }
}
