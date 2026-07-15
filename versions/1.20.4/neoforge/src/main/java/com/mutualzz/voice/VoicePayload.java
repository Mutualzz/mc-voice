package com.mutualzz.voice;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.nio.charset.StandardCharsets;

/** NeoForge 1.20.4 payload shape (id()/write, not Type/StreamCodec). */
public record VoicePayload(String json) implements CustomPacketPayload {
    public static final ResourceLocation ID = new ResourceLocation("mutualzz", "voice");

    public VoicePayload(FriendlyByteBuf buf) {
        this(readJson(buf));
    }

    private static String readJson(FriendlyByteBuf buf) {
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return new String(data, StandardCharsets.UTF_8);
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBytes(json.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public ResourceLocation id() {
        return ID;
    }
}
