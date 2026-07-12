package com.mutualzz.voice;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

import java.nio.charset.StandardCharsets;

public record VoicePayload(String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<VoicePayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath("mutualzz", "voice"));

    public static final StreamCodec<FriendlyByteBuf, VoicePayload> CODEC = StreamCodec.of(
            (buf, value) -> buf.writeBytes(value.json.getBytes(StandardCharsets.UTF_8)),
            buf -> {
                byte[] data = new byte[buf.readableBytes()];
                buf.readBytes(data);
                return new VoicePayload(new String(data, StandardCharsets.UTF_8));
            }
    );

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(TYPE, CODEC);
        PayloadTypeRegistry.serverboundPlay().register(TYPE, CODEC);
    }

    /** Tell the Paper plugin (and hub) about mute/deafen so the Mutualzz app UI updates. */
    public static void sendVoiceState(boolean selfMute, boolean selfDeaf) {
        String json = "{\"t\":\"voice_state\",\"selfMute\":" + selfMute
                + ",\"selfDeaf\":" + selfDeaf + "}";
        ClientPlayNetworking.send(new VoicePayload(json));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
