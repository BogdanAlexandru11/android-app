package com.google.android.gms.location.sample.locationupdatesforegroundservice;

/**
 * Created by Juned on 9/15/2017.
 */

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.Timer;
import java.util.TimerTask;


public class FloatingWidgetShowService extends Service {

    WindowManager windowManager;
    View floatingView, collapsedView;
    WindowManager.LayoutParams params ;
    Context context;
    private static String myString;
    CountDownTimer cTimer = null;
    private static final String TAG = FloatingWidgetShowService.class.getSimpleName();
    private static boolean alreadyStarted=false;


    public Handler mHandler;
    public FloatingWidgetShowService(Context context) {
        this.context=context;
    }

    public FloatingWidgetShowService (){
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
        Handler handler = new Handler(Looper.getMainLooper());
        Timer timer = new Timer();
        timer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                handler.postDelayed(() -> {
                    if(myString!=null && !alreadyStarted){
                        cancelTimer();
                        startTimer(floatingView);
                    }
                }, 0 );
            }
        }, 0, 1000);
    }

    void startTimer(View view) {
        cTimer = new CountDownTimer(6000, 1000) {
            public void onTick(long millisUntilFinished) {
            floatingView.findViewById(R.id.van).setVisibility(View.VISIBLE);
            floatingView.findViewById(R.id.Logo_Icon).setVisibility(View.INVISIBLE);
            alreadyStarted=true;

            }
            public void onFinish() {
            floatingView.findViewById(R.id.van).setVisibility(View.INVISIBLE);
            floatingView.findViewById(R.id.Logo_Icon).setVisibility(View.VISIBLE);
            alreadyStarted=false;
            }
        };
        cTimer.start();
    }

    //cancel timer
    void cancelTimer() {
        if(cTimer!=null){
            floatingView.findViewById(R.id.van).setVisibility(View.INVISIBLE);
            floatingView.findViewById(R.id.Logo_Icon).setVisibility(View.VISIBLE);
            cTimer.cancel();
        }
    }

    public BroadcastReceiver reciever = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            myString = intent.getStringExtra("valueForFloatingWidget");
            Log.d(TAG, myString);
            if(myString.equalsIgnoreCase("null")){
                myString=null;
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) windowManager.removeView(floatingView);
    }
}