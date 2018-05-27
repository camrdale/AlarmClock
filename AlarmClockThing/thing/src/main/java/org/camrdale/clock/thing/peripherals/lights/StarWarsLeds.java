package org.camrdale.clock.thing.peripherals.lights;

import android.graphics.Color;
import android.os.CountDownTimer;
import android.util.Log;

import java.io.IOException;

public class StarWarsLeds extends CountDownTimer {
    private static final String TAG = StarWarsLeds.class.getSimpleName();

    private int ledCount = 0;
    private int[] rainbow = {
            Color.valueOf(0.0f, 0.0f, 1.0f, 0.0f).toArgb(),
            Color.valueOf(0.0f, 0.0f, 1.0f, 0.0f).toArgb(),
            Color.valueOf(0.0f, 0.0f, 1.0f, 0.0f).toArgb(),
            Color.valueOf(0.0f, 0.0f, 1.0f, 0.0f).toArgb(),
            Color.valueOf(0.0f, 0.0f, 1.0f, 0.0f).toArgb(),
            Color.valueOf(0.0f, 0.0f, 1.0f, 0.0f).toArgb(),
            Color.valueOf(0.0f, 0.0f, 1.0f, 0.0f).toArgb()};

    private Apa102 ledStrip;
    private boolean skipNext = false;

    StarWarsLeds(Apa102 ledStrip) {
        super(Long.MAX_VALUE, 500);
        this.ledStrip = ledStrip;
    }

    public void onTick(long millisUntilFinished) {
        nextCycle();
    }

    public void onFinish() {
        start();
    }

    private void nextCycle() {
        if (skipNext) {
            skipNext = false;
            return;
        }
        int leftLed = 3 - ledCount;
        int rightLed = 3 + ledCount;

        Color currentColor = Color.valueOf(rainbow[leftLed]);
        boolean turnOn = currentColor.alpha() < 0.5;
        Color newColor = Color.valueOf(currentColor.red(), currentColor.green(), currentColor.blue(),
                Math.max(Math.min(1.0f - currentColor.alpha(), 1.0f), 0.0f));
        rainbow[leftLed] = newColor.toArgb();

        if (leftLed != rightLed) {
            currentColor = Color.valueOf(rainbow[rightLed]);
            newColor = Color.valueOf(currentColor.red(), currentColor.green(), currentColor.blue(),
                    Math.max(Math.min(1.0f - currentColor.alpha(), 1.0f), 0.0f));
            rainbow[rightLed] = newColor.toArgb();
        }

        if (turnOn) {
            ledCount++;
        } else {
            ledCount--;
        }
        if (ledCount > 3) {
            ledCount = 3;
            skipNext = true;
        }
        if (ledCount < 0) {
            ledCount = 0;
        }

        try {
            ledStrip.write(rainbow);
        } catch (IOException e) {
            Log.e(TAG, "Failed to light the LED strip.", e);
        }
    }
}
