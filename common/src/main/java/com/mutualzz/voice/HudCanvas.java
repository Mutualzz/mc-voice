package com.mutualzz.voice;

/**
 * Thin HUD drawing abstraction so Fabric / NeoForge / Forge / older MC lines
 * can wrap GuiGraphics, GuiGraphicsExtractor, or PoseStack without sharing those types in common.
 */
@FunctionalInterface
public interface HudCanvas {
    void drawText(String text, int x, int y, int color, boolean shadow);
}
