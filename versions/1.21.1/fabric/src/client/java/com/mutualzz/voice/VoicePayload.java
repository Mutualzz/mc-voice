package com.mutualzz.voice;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;

public record VoicePayload(String json) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<VoicePayload> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("mutualzz", "voice"));

    public static final StreamCodec<FriendlyByteBuf, VoicePayload> CODEC = StreamCodec.of(
            (buf, value) -> buf.writeBytes(value.json.getBytes(StandardCharsets.UTF_8)),
            buf -> {
                byte[] data = new byte[buf.readableBytes()];
                buf.readBytes(data);
                return new VoicePayload(new String(data, StandardCharsets.UTF_8));
            }
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
