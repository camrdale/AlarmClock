package org.camrdale.clock;

import dagger.Component;
import dagger.android.AndroidInjectionModule;
import dagger.android.AndroidInjector;

@Component(modules = { AndroidInjectionModule.class, AlarmClockApplicationModule.class})
public interface AlarmClockApplicationComponent extends AndroidInjector<AlarmClockApplication> {
}
