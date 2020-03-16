package com.google.android.gms.location.sample.locationupdatesforegroundservice;

import android.content.ComponentCallbacks2;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

interface LocationUpdatesServiceInterface extends ComponentCallbacks2 {
    void onCreate();

    int onStartCommand(Intent intent, int flags, int startId);

    @Override
    void onConfigurationChanged(Configuration newConfig);

    IBinder onBind(Intent intent);

    void onRebind(Intent intent);

    boolean onUnbind(Intent intent);

    void onDestroy();

    void requestLocationUpdates();

    void removeLocationUpdates();

    View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState);

    boolean serviceIsRunningInForeground(Context context);
}
