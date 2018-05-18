package org.camrdale.clock.thing.alarm;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.common.collect.ImmutableSet;

import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class AlarmStorage {
    private static final String TAG = AlarmStorage.class.getSimpleName();

    private static final String PREFERENCES_NAME = "org.camrdale.clock.ALARM_PREFERENCES";
    private static final String PREF_ALARMS_KEY = "currentAlarms";
    private static final String DEFAULT_ALARM = "30 8 * * MON-FRI,0";
    private static final String PREF_WEB_KEY_KEY = "webKey";

    private SharedPreferences preferences;
    private Set<String> alarmSaveStrings;
    private String webKey;

    @Inject AlarmStorage(Context context) {
        preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
        alarmSaveStrings =
                preferences.getStringSet(PREF_ALARMS_KEY, ImmutableSet.of(DEFAULT_ALARM));
        Log.i(TAG, "Loaded alarms from storage: " + alarmSaveStrings);
        webKey = preferences.getString(PREF_WEB_KEY_KEY, null);
        Log.i(TAG, "Loaded webKey from storage: " + webKey);
    }

    public Set<String> getAlarms() {
        return alarmSaveStrings;
    }

    public void saveNewAlarms(Set<String> alarmSaveStrings) {
        this.alarmSaveStrings = alarmSaveStrings;
        Log.i(TAG, "Saving new alarms to storage: " + alarmSaveStrings);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putStringSet(PREF_ALARMS_KEY, alarmSaveStrings);
        editor.apply();
    }

    public Optional<String> getWebKey() {
        return Optional.ofNullable(webKey);
    }

    public void saveNewWebKey(String webKey) {
        this.webKey = webKey;
        Log.i(TAG, "Saving new webKey to storage: " + webKey);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(PREF_WEB_KEY_KEY, webKey);
        editor.apply();
    }
}
