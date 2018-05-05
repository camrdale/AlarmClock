package org.camrdale.clock;

import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.parser.CronParser;

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

    @Provides
    CronParser provideCronParser() {
        return new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.UNIX));
    }
}
