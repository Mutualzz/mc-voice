package com.mutualzz.voice;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public final class MutualzzVoiceClient implements ClientModInitializer {
    private static final VoiceClientController.Keys KEYS = new VoiceClientController.Keys();

    @Override
    public void onInitializeClient() {
        VoiceSettings.get().load();

        PayloadTypeRegistry.playC2S().register(VoicePayload.TYPE, VoicePayload.CODEC);
        PayloadTypeRegistry.playS2C().register(VoicePayload.TYPE, VoicePayload.CODEC);
        VoiceNetworking.setBackend(json -> ClientPlayNetworking.send(new VoicePayload(json)));

        ClientPlayNetworking.registerGlobalReceiver(VoicePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> VoiceSession.handlePluginMessage(payload.json()));
        });

        KEYS.mute = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.mutualzz_voice.mute",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                VoiceClientController.CATEGORY
        ));
        KEYS.deafen = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.mutualzz_voice.deafen",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_D,
                VoiceClientController.CATEGORY
        ));
        KEYS.speakingHud = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.mutualzz_voice.speaking_hud",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                VoiceClientController.CATEGORY
        ));
        KEYS.ptt = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.mutualzz_voice.ptt",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                VoiceClientController.CATEGORY
        ));
        KEYS.inputMode = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.mutualzz_voice.input_mode",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_I,
                VoiceClientController.CATEGORY
        ));
        KEYS.volumeUp = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.mutualzz_voice.volume_up",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_EQUAL,
                VoiceClientController.CATEGORY
        ));
        KEYS.volumeDown = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.mutualzz_voice.volume_down",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_MINUS,
                VoiceClientController.CATEGORY
        ));
        KEYS.micSensUp = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.mutualzz_voice.mic_sens_up",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_EQUAL,
                VoiceClientController.CATEGORY
        ));
        KEYS.micSensDown = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.mutualzz_voice.mic_sens_down",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_MINUS,
                VoiceClientController.CATEGORY
        ));
        KEYS.selectNext = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.mutualzz_voice.select_next",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_BRACKET,
                VoiceClientController.CATEGORY
        ));
        KEYS.selectPrev = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.mutualzz_voice.select_prev",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_BRACKET,
                VoiceClientController.CATEGORY
        ));
        KEYS.userVolumeUp = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.mutualzz_voice.user_volume_up",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UP,
                VoiceClientController.CATEGORY
        ));
        KEYS.userVolumeDown = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.mutualzz_voice.user_volume_down",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_DOWN,
                VoiceClientController.CATEGORY
        ));
        KEYS.userMute = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.mutualzz_voice.user_mute",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                VoiceClientController.CATEGORY
        ));
        KEYS.settings = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.mutualzz_voice.settings",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                VoiceClientController.CATEGORY
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> VoiceClientController.onClientTick(client, KEYS));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> VoiceSession.leave());

        HudRenderCallback.EVENT.register((graphics, tickDelta) -> {
            var font = net.minecraft.client.Minecraft.getInstance().font;
            VoiceClientController.renderHud((text, x, y, color, shadow) ->
                    graphics.drawString(font, text, x, y, color, shadow));
        });
    }
}
