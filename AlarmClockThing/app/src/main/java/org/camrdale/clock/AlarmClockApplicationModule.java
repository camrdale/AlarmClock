package org.camrdale.clock;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;

@Module
public abstract class AlarmClockApplicationModule {
    @ContributesAndroidInjector
    abstract HomeActivity contributeHomeActivityInjector();
}
