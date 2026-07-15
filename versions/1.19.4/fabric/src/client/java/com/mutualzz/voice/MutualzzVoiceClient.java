package com.mutualzz.voice;

import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.glfw.GLFW;

import java.nio.charset.StandardCharsets;

/** Fabric 1.19.x / 1.18.x — Identifier channel + PoseStack HUD. */
public final class MutualzzVoiceClient implements ClientModInitializer {
    private static final ResourceLocation CHANNEL = new ResourceLocation("mutualzz", "voice");
    private static final VoiceClientController.Keys KEYS = new VoiceClientController.Keys();

    @Override
    public void onInitializeClient() {
        VoiceSettings.get().load();

        VoiceNetworking.setBackend(MutualzzVoiceClient::sendJson);
        ClientPlayNetworking.registerGlobalReceiver(CHANNEL, (client, handler, buf, responseSender) -> {
            byte[] data = new byte[buf.readableBytes()];
            buf.readBytes(data);
            String json = new String(data, StandardCharsets.UTF_8);
            client.execute(() -> VoiceSession.handlePluginMessage(json));
        });

        KEYS.mute = bind("key.mutualzz_voice.mute", GLFW.GLFW_KEY_M);
        KEYS.deafen = bind("key.mutualzz_voice.deafen", GLFW.GLFW_KEY_D);
        KEYS.speakingHud = bind("key.mutualzz_voice.speaking_hud", GLFW.GLFW_KEY_H);
        KEYS.ptt = bind("key.mutualzz_voice.ptt", GLFW.GLFW_KEY_V);
        KEYS.inputMode = bind("key.mutualzz_voice.input_mode", GLFW.GLFW_KEY_I);
        KEYS.volumeUp = bind("key.mutualzz_voice.volume_up", GLFW.GLFW_KEY_EQUAL);
        KEYS.volumeDown = bind("key.mutualzz_voice.volume_down", GLFW.GLFW_KEY_MINUS);
        KEYS.micSensUp = bind("key.mutualzz_voice.mic_sens_up", GLFW.GLFW_KEY_EQUAL);
        KEYS.micSensDown = bind("key.mutualzz_voice.mic_sens_down", GLFW.GLFW_KEY_MINUS);
        KEYS.selectNext = bind("key.mutualzz_voice.select_next", GLFW.GLFW_KEY_RIGHT_BRACKET);
        KEYS.selectPrev = bind("key.mutualzz_voice.select_prev", GLFW.GLFW_KEY_LEFT_BRACKET);
        KEYS.userVolumeUp = bind("key.mutualzz_voice.user_volume_up", GLFW.GLFW_KEY_UP);
        KEYS.userVolumeDown = bind("key.mutualzz_voice.user_volume_down", GLFW.GLFW_KEY_DOWN);
        KEYS.userMute = bind("key.mutualzz_voice.user_mute", GLFW.GLFW_KEY_U);
        KEYS.settings = bind("key.mutualzz_voice.settings", GLFW.GLFW_KEY_O);

        ClientTickEvents.END_CLIENT_TICK.register(client -> VoiceClientController.onClientTick(client, KEYS));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> VoiceSession.leave());

        HudRenderCallback.EVENT.register((poseStack, tickDelta) -> {
            var font = net.minecraft.client.Minecraft.getInstance().font;
            VoiceClientController.renderHud((text, x, y, color, shadow) -> {
                if (shadow) {
                    font.drawShadow(poseStack, text, x, y, color);
                } else {
                    font.draw(poseStack, text, x, y, color);
                }
            });
        });
    }

    private static KeyMapping bind(String name, int key) {
        return KeyBindingHelper.registerKeyBinding(new KeyMapping(
                name,
                InputConstants.Type.KEYSYM,
                key,
                VoiceClientController.CATEGORY
        ));
    }

    private static void sendJson(String json) {
        FriendlyByteBuf buf = PacketByteBufs.create();
        buf.writeBytes(json.getBytes(StandardCharsets.UTF_8));
        ClientPlayNetworking.send(CHANNEL, buf);
    }
}
