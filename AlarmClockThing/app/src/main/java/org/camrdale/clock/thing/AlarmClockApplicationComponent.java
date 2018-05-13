package org.camrdale.clock.thing;

import android.content.Context;

import org.camrdale.clock.thing.web.WebModule;

import javax.inject.Singleton;

import dagger.Component;
import dagger.android.AndroidInjectionModule;
import dagger.android.AndroidInjector;

@Component(modules = {
        AndroidInjectionModule.class,
        AlarmClockApplicationModule.class,
        WebModule.class,
})
@Singleton
public interface AlarmClockApplicationComponent extends AndroidInjector<AlarmClockApplication> {
    Context context();
}
