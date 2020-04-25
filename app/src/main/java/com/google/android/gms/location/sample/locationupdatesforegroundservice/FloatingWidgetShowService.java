package com.google.android.gms.location.sample.locationupdatesforegroundservice;

/**
 * Created by Juned on 9/15/2017.
 */

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Objects;


public class FloatingWidgetShowService extends Service{


    WindowManager windowManager;
    View floatingView, collapsedView;
    WindowManager.LayoutParams params ;
    public FloatingWidgetShowService() {
    }

    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_widget_layout, null);

        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(floatingView, params);

        collapsedView = floatingView.findViewById(R.id.Layout_Collapsed);
        floatingView.findViewById(R.id.Widget_Close_Icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                stopSelf();
            }
        });
        floatingView.findViewById(R.id.MainParentRelativeLayout).setOnTouchListener(new View.OnTouchListener() {
            int X_Axis, Y_Axis;
            float TouchX, TouchY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        X_Axis = params.x;
                        Y_Axis = params.y;
                        TouchX = event.getRawX();
                        TouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = X_Axis + (int) (event.getRawX() - TouchX);
                        params.y = Y_Axis + (int) (event.getRawY() - TouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) windowManager.removeView(floatingView);
    }

    public static class ExampleBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d("alerttttttttttttttttt", Objects.requireNonNull(intent.getStringExtra("valueForFloatingWidget")));
            View view = View.inflate(context, R.layout.floating_widget_layout, null);
//            Log.d("alert","view is "+);
            try{
//                view.findViewById(R.id.van).setVisibility(View.VISIBLE);
                view.findViewById(R.id.speedvan).setVisibility(View.GONE);

            }
            catch (Exception e){
                Log.d("alert","error setting the view");
            }
        }

    }
}