package org.camrdale.clock.thing.peripherals.lights;

import android.graphics.Color;
import android.os.CountDownTimer;
import android.util.Log;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

public class KnightRiderLeds extends CountDownTimer {
    private static final String TAG = KnightRiderLeds.class.getSimpleName();

    private static final int[] LEDS = {0, 1, 2, 3, 4, 5, 6, 5, 4, 3, 2, 1};
    private static final float[][] LED_CYCLE_VALUES = {
            {0.1f, 0.9f, -0.3f, -0.3f, -0.3f, -0.3f, -0.3f},
            {0.0f, 0.0f, -0.3f, -0.3f, -0.3f, -0.3f, -0.3f}};

    private int ledCount = 0;
    private int ledCycleCount = 0;
    private List<Integer> latestLeds = new LinkedList<>();
    private int[] rainbow = {
            Color.valueOf(1.0f, 0.0f, 0.0f, 0.0f).toArgb(),
            Color.valueOf(1.0f, 0.0f, 0.0f, 0.0f).toArgb(),
            Color.valueOf(1.0f, 0.0f, 0.0f, 0.0f).toArgb(),
            Color.valueOf(1.0f, 0.0f, 0.0f, 0.0f).toArgb(),
            Color.valueOf(1.0f, 0.0f, 0.0f, 0.0f).toArgb(),
            Color.valueOf(1.0f, 0.0f, 0.0f, 0.0f).toArgb(),
            Color.valueOf(1.0f, 0.0f, 0.0f, 0.0f).toArgb()};

    private Apa102 ledStrip;

    KnightRiderLeds(Apa102 ledStrip) {
        super(Long.MAX_VALUE, 50);
        this.ledStrip = ledStrip;
    }

    public void onTick(long millisUntilFinished) {
        nextCycle();
    }

    public void onFinish() {
        start();
    }

    private void nextCycle() {
        for (int i = 0; i < latestLeds.size(); i++) {
            int currentLed = latestLeds.get(i);
            Color currentColor = Color.valueOf(rainbow[currentLed]);
            float delta = LED_CYCLE_VALUES[ledCycleCount][i];
            Color newColor = Color.valueOf(currentColor.red(), currentColor.green(), currentColor.blue(),
                    Math.max(Math.min(currentColor.alpha() + delta, 1.0f), 0.0f));
            rainbow[currentLed] = newColor.toArgb();
        }
        ledCycleCount++;
        if (ledCycleCount == LED_CYCLE_VALUES.length) {
            ledCycleCount = 0;
            int newLed = LEDS[ledCount];
            latestLeds.remove(Integer.valueOf(newLed));
            latestLeds.add(0, newLed);

            ledCount++;
            if (ledCount == LEDS.length) {
                ledCount = 0;
            }
        }
        try {
            ledStrip.write(rainbow);
        } catch (IOException e) {
            Log.e(TAG, "Failed to light the LED strip.", e);
        }
    }
}
