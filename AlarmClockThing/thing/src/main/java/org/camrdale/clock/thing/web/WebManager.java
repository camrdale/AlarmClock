package org.camrdale.clock.thing.web;

import android.content.Context;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;

import java.util.function.Consumer;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class WebManager {
    private static final String TAG = WebManager.class.getSimpleName();

    private final Gson gson;
    private final Context context;
    private RequestQueue queue;

    @Inject WebManager(Gson gson, Context context) {
        this.gson = gson;
        this.context = context;
        queue = Volley.newRequestQueue(context);
    }

    public void register(String verificationNumber, Consumer<RegisterResponse> onResponse) {
        GsonRequest<RegisterRequest, RegisterResponse> gsonRequest = new GsonRequest<>(
                gson,
                "https://clock.camrdale.org/register",
                RegisterResponse.class,
                ImmutableMap.of(),
                new RegisterRequest(verificationNumber),
                onResponse::accept,
                this::onError);
        queue.add(gsonRequest);
    }

    public void checkIn(String clockKey, Consumer<CheckInResponse> onResponse) {
        GsonRequest<CheckInRequest, CheckInResponse> gsonRequest = new GsonRequest<>(
                gson,
                "https://clock.camrdale.org/checkin",
                CheckInResponse.class,
                ImmutableMap.of(),
                new CheckInRequest(clockKey),
                onResponse::accept,
                this::onError);
        queue.add(gsonRequest);
    }

    private void onError(VolleyError error) {
        Log.e(TAG, "Error calling server", error);
    }

    public void cleanup() {
        if (queue != null) {
            queue.stop();
        }
    }
}
