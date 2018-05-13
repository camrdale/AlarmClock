package org.camrdale.clock.thing;

import android.content.Context;
import android.support.annotation.NonNull;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;

import org.camrdale.clock.thing.HomeActivity;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.android.ContributesAndroidInjector;

@Module(includes = AlarmClockApplicationModule.Activities.class)
final class AlarmClockApplicationModule {

    @Module
    interface Activities {
        @ContributesAndroidInjector
        HomeActivity contributeHomeActivityInjector();
    }

    private Context appContext;

    public AlarmClockApplicationModule(@NonNull Context context) {
        appContext = context;
    }

    @Provides
    @Singleton
    public Context provideContext() {
        return appContext;
    }

    @Provides
    CronParser provideCronParser() {
        return new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
    }
}
