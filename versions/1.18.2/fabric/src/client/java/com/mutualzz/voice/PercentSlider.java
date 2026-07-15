package com.mutualzz.voice;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;

import java.util.function.Consumer;
import java.util.function.DoubleSupplier;

/**
 * Slider mapped from a real float range onto AbstractSliderButton's 0..1 value.
 */
public final class PercentSlider extends AbstractSliderButton {
    private final String label;
    private final float min;
    private final float max;
    private final Consumer<Float> onChange;
    private final DoubleSupplier current;

    public PercentSlider(
            int x,
            int y,
            int width,
            int height,
            String label,
            float min,
            float max,
            DoubleSupplier current,
            Consumer<Float> onChange
    ) {
        super(x, y, width, height, net.minecraft.network.chat.TextComponent.EMPTY, toProgress(current.getAsDouble(), min, max));
        this.label = label;
        this.min = min;
        this.max = max;
        this.current = current;
        this.onChange = onChange;
        updateMessage();
    }

    public void refreshFromSettings() {
        this.value = toProgress(current.getAsDouble(), min, max);
        updateMessage();
    }

    @Override
    protected void updateMessage() {
        int pct = Math.round(fromProgress(this.value) * 100f);
        setMessage(new net.minecraft.network.chat.TextComponent(label + ": " + pct + "%"));
    }

    @Override
    protected void applyValue() {
        onChange.accept(fromProgress(this.value));
        updateMessage();
    }

    private float fromProgress(double progress) {
        return (float) (min + (max - min) * progress);
    }

    private static double toProgress(double value, float min, float max) {
        if (max <= min) return 0;
        return Math.max(0, Math.min(1, (value - min) / (max - min)));
    }
}
