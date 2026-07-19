package com.mutualzz.voice;

import de.siphalor.amecs.key_modifiers.api.AmecsKeyMappingWithKeyModifiers;
import de.siphalor.amecs.key_modifiers.api.AmecsKeyModifierCombination;
import de.siphalor.amecs.key_modifiers.api.AmecsKeyModifiers;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public final class MutualzzVoiceClient implements ClientModInitializer {
    private static final AmecsKeyModifierCombination ALT =
            new AmecsKeyModifierCombination(AmecsKeyModifiers.ALT);
    private static final AmecsKeyModifierCombination ALT_SHIFT =
            new AmecsKeyModifierCombination(AmecsKeyModifiers.ALT, AmecsKeyModifiers.SHIFT);

    private static final VoiceClientController.Keys KEYS = new VoiceClientController.Keys();

    @Override
    public void onInitializeClient() {
        VoiceSettings.get().load();

        PayloadTypeRegistry.clientboundPlay().register(VoicePayload.TYPE, VoicePayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(VoicePayload.TYPE, VoicePayload.CODEC);
        VoiceNetworking.setBackend(json -> ClientPlayNetworking.send(new VoicePayload(json)));

        ClientPlayNetworking.registerGlobalReceiver(VoicePayload.TYPE, (payload, context) -> {
            context.client().execute(() -> VoiceSession.handlePluginMessage(payload.json()));
        });

        KEYS.mute = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.mute",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_M,
                VoiceClientController.CATEGORY,
                ALT
        ));
        KEYS.deafen = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.deafen",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_D,
                VoiceClientController.CATEGORY,
                ALT_SHIFT
        ));
        KEYS.speakingHud = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.speaking_hud",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_H,
                VoiceClientController.CATEGORY,
                ALT
        ));
        KEYS.ptt = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.mutualzz_voice.ptt",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                VoiceClientController.CATEGORY
        ));
        KEYS.inputMode = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.input_mode",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_I,
                VoiceClientController.CATEGORY,
                ALT
        ));
        KEYS.volumeUp = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.volume_up",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_EQUAL,
                VoiceClientController.CATEGORY,
                ALT
        ));
        KEYS.volumeDown = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.volume_down",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_MINUS,
                VoiceClientController.CATEGORY,
                ALT
        ));
        KEYS.micSensUp = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.mic_sens_up",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_EQUAL,
                VoiceClientController.CATEGORY,
                ALT_SHIFT
        ));
        KEYS.micSensDown = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.mic_sens_down",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_MINUS,
                VoiceClientController.CATEGORY,
                ALT_SHIFT
        ));
        KEYS.selectNext = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.select_next",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_BRACKET,
                VoiceClientController.CATEGORY,
                ALT
        ));
        KEYS.selectPrev = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.select_prev",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_LEFT_BRACKET,
                VoiceClientController.CATEGORY,
                ALT
        ));
        KEYS.userVolumeUp = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.user_volume_up",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_UP,
                VoiceClientController.CATEGORY,
                ALT
        ));
        KEYS.userVolumeDown = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.user_volume_down",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_DOWN,
                VoiceClientController.CATEGORY,
                ALT
        ));
        KEYS.userMute = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.user_mute",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_U,
                VoiceClientController.CATEGORY,
                ALT
        ));
        KEYS.settings = KeyMappingHelper.registerKeyMapping(new AmecsKeyMappingWithKeyModifiers(
                "key.mutualzz_voice.settings",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_O,
                VoiceClientController.CATEGORY,
                ALT
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> VoiceClientController.onClientTick(client, KEYS));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> VoiceSession.leave());

        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath(VoiceClientController.MOD_ID, "status"),
                (graphics, tickCounter) -> {
                    var font = net.minecraft.client.Minecraft.getInstance().font;
                    VoiceClientController.renderHud((text, x, y, color, shadow) ->
                            graphics.text(font, text, x, y, color, shadow));
                }
        );
    }
}
