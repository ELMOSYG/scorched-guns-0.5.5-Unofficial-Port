package top.ribs.scguns.debug.client.screen.widget;

import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import top.ribs.scguns.debug.IDebugWidget;

import java.util.function.Consumer;

/**
 * Debug screen slider.
 *
 * <p>0.5.5 extended NeoForge's {@code ForgeSlider}, which no longer exists in
 * 1.21. Vanilla {@link AbstractSliderButton} covers the same ground, so this is a
 * plain slider that maps the 0..1 button value onto the configured range and
 * reports changes through the callback. The original custom rendering is gone
 * because the vanilla button draws the modern widget sprites itself.</p>
 */
public class DebugSlider extends AbstractSliderButton implements IDebugWidget {
    private final double minValue;
    private final double maxValue;
    private final double stepSize;
    private final int precision;
    private final Consumer<Double> callback;

    public DebugSlider(double minValue, double maxValue, double currentValue, double stepSize,
                       int precision, Consumer<Double> callback) {
        super(0, 0, 0, 14, Component.empty(),
                maxValue == minValue ? 0.0D : (currentValue - minValue) / (maxValue - minValue));
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.stepSize = stepSize;
        this.precision = precision;
        this.callback = callback;
        this.updateMessage();
    }

    /** The slider value mapped back onto the configured range. */
    public double getValue() {
        double range = this.maxValue - this.minValue;
        if (this.stepSize > 0.0D) {
            double steps = Math.round(this.value * range / this.stepSize);
            return this.minValue + steps * this.stepSize;
        }
        return this.minValue + this.value * range;
    }

    @Override
    protected void updateMessage() {
        this.setMessage(Component.literal(String.format("%." + Math.max(0, this.precision) + "f", this.getValue())));
    }

    @Override
    protected void applyValue() {
        this.callback.accept(this.getValue());
    }
}
