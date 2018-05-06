package org.camrdale.clock;

import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;

import org.camrdale.clock.peripherals.ButtonManager;
import org.camrdale.clock.peripherals.DisplayManager;
import org.camrdale.clock.peripherals.LedManager;
import org.camrdale.clock.state.StateManager;

import javax.inject.Inject;

import dagger.android.AndroidInjection;

/**
 * Skeleton of an Android Things activity.
 */
public class HomeActivity extends Activity {
    private static final String TAG = HomeActivity.class.getSimpleName();

    @Inject DisplayManager displayManager;
    @Inject LedManager ledManager;
    @Inject ButtonManager buttonManager;
    @Inject StateManager stateManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AndroidInjection.inject(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        displayManager.initialize();
        ledManager.initialize();
        buttonManager.initialize();
        stateManager.initialize(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_A) {
            ledManager.setRedLed(true);
            displayManager.setDisplayMode(DisplayManager.DisplayMode.MINUTE_SECOND);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_B) {
            ledManager.setGreenLed(true);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_C) {
            ledManager.setBlueLed(true);
            if (stateManager.getCurrentState() == StateManager.AlarmState.IDLE) {
                displayManager.setDisplayMode(DisplayManager.DisplayMode.MONTH_DAY);
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_A) {
            ledManager.setRedLed(false);
            displayManager.setDisplayMode(DisplayManager.DisplayMode.HOUR_MINUTE);
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_B) {
            ledManager.setGreenLed(false);
            stateManager.sleepKeyPress();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_C) {
            ledManager.setBlueLed(false);
            stateManager.snoozeKeyPress();
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stateManager.cleanup();
        displayManager.cleanup();
        ledManager.cleanup();
        buttonManager.cleanup();
    }
}
