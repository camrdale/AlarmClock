package org.camrdale.clock.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import dagger.Module;
import dagger.Provides;

@Module
public class WebModule {
    @Provides
    Gson provideGson() {
        return new GsonBuilder().create();
    }
}
