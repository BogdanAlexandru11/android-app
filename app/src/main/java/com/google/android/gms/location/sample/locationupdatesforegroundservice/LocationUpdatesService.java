/**
 * Copyright 2017 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.location.sample.locationupdatesforegroundservice;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.maps.android.PolyUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * A bound and started service that is promoted to a foreground service when location updates have
 * been requested and all clients unbind.
 *
 * For apps running in the background on "O" devices, location is computed only once every 10
 * minutes and delivered batched every 30 minutes. This restriction applies even to apps
 * targeting "N" or lower which are run on "O" devices.
 *
 * This sample show how to use a long-running service for location updates. When an activity is
 * bound to this service, frequent location updates are permitted. When the activity is removed
 * from the foreground, the service promotes itself to a foreground service, and location updates
 * continue. When the activity comes back to the foreground, the foreground service stops, and the
 * notification assocaited with that service is removed.
 */
public class LocationUpdatesService extends Service{

    private static final String PACKAGE_NAME =
            "com.google.android.gms.location.sample.locationupdatesforegroundservice";

    private static final String TAG = LocationUpdatesService.class.getSimpleName();

    /**
     * The name of the channel for notifications.
     */
    private static final String CHANNEL_ID = "channel_01";

    static final String ACTION_BROADCAST = PACKAGE_NAME + ".broadcast";

    static final String EXTRA_LOCATION = PACKAGE_NAME + ".location";
    private static final String EXTRA_STARTED_FROM_NOTIFICATION = PACKAGE_NAME +
            ".started_from_notification";

    private final IBinder mBinder = new LocalBinder();

    /**
     * The desired interval for location updates. Inexact. Updates may be more or less frequent.
     */
    private static final long UPDATE_INTERVAL_IN_MILLISECONDS = 10000;

    /**
     * The fastest rate for active location updates. Updates will never be more frequent
     * than this value.
     */
    private static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2;

    /**
     * The identifier for the notification displayed for the foreground service.
     */
    private static final int NOTIFICATION_ID = 12345678;

    /**
     * Used to check whether the bound activity has really gone away and not unbound as part of an
     * orientation change. We create a foreground service notification only if the former takes
     * place.
     */
    private boolean mChangingConfiguration = false;

    private NotificationManager mNotificationManager;

    /**
     * Contains parameters used by {@link com.google.android.gms.location.FusedLocationProviderApi}.
     */
    private LocationRequest mLocationRequest;

    /**
     * Provides access to the Fused Location Provider API.
     */
    private FusedLocationProviderClient mFusedLocationClient;

    /**
     * Callback for changes in location.
     */
    private LocationCallback mLocationCallback;

    private Handler mServiceHandler;
    private ArrayList<List<LatLng>> listOLists = new ArrayList<>();

    ImageView speedVan;
    ImageView free;

    /**
     * The current location.
     */
    private Location mLocation;
    private PolyUtil test;
    public LocationUpdatesService() {
    }

    @Override
    public void onCreate() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        mLocationCallback = new LocationCallback() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());
            }
        };

        createLocationRequest();
        getLastLocation();

        HandlerThread handlerThread = new HandlerThread(TAG);
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Android O requires a Notification Channel.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.app_name);
            // Create the channel for the notification
            NotificationChannel mChannel =
                    new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_DEFAULT);

            // Set the Notification Channel for the Notification Manager.
            mNotificationManager.createNotificationChannel(mChannel);
        }

        List<LatLng> carlow1 = PolyUtil.decode("i~oaIhx`h@UvCGr@Ab@AN?N?V?PBVBNBJDLHJTZDFPXNTP^LZXt@`@~@\\l@`@l@HJBBFHNLb@XXTPPJLLNDHDLJZHXRt@Nj@J`@BHJf@Nf@V~@XtAJ`@Nj@FVBNHd@B^@R@X?h@?f@?h@?F@l@?\\@p@@~AJzFJzI?PH~EFdC@pA@n@Bf@BdA@hA@`AA|AEzAC~@A`@?HA^Aj@Cj@IrAA\\AR?LCr@?LCb@KvD?@ElBEjAGzACb@C`@Gp@Kn@]pCGl@o@rF}@rHs@jFMx@o@jEc@nCi@lD[jBYnAI\\ERGVKZKZGROf@K^Kf@Q~@Ov@Gb@QnAc@bDm@jEu@lFMt@WhB]nCAHSzASdBOlAOrAYjBSjAIb@UnAE^");
        List<LatLng> carlow2 = PolyUtil.decode("yipaIjiei@A@A?A?C?AAA?A?AAECAAAAACAAAC?CACAC?CAC?C?C?C?C?C?E?C@C@G?C@A@C@C@A@C@A@A@A@AB?@A@?B?@?B?B@B@@@@@@@@B@@@B@B@@?B@B?B@B?B?D?B?B?B?BJRHTFPN^LXfAbB\\d@n@|@RRfAx@bBnAFDNJrAhATRb@^jA`A`@\\XTDD~@|@h@f@RRhAfA^ZZZLJl@j@\\`@@@v@hAZb@Zb@~@pA^f@Zd@h@v@^l@f@p@t@rAh@x@DFHLHNFLPZDT@Dz@bBz@jBbArB`ArBv@fABBt@x@`@h@JNTZ`@j@DDZb@b@h@BDPX`@r@@@DHTN@A@??A@A@?@??A@?@?@?@??@@?@??@@??@@@@@?@@@?@@B?@?@?B@@?@?B?DA@?@?@A@?@?@A@?BJl@DPLb@DNHVBFLd@DPFt@D\\Dr@@L@t@B`B?pC?h@?b@@v@@bADxA?D@|@B`@BTDTJb@");
        List<LatLng> carlow3 = PolyUtil.decode("wcnaI`~gi@A??@A?A?A?A?A?A?A??AA??AA??AA??AAA?AA??A?AAA?A?A?A?A?AAA?A?A?A@A?A?A?A?A@A?A?A@A?A@A?A@??A@A@??A@A@?@??A@?@?@?@??@@?@??@@??@@@@@?@@@?@@B?@?@?B@@?@?B?DA@?@?@A@?@?@A@?BJl@DPLb@DNHVBFLd@DPFt@D\\Dr@@L@t@B`B?pC?h@?b@@v@@bADxA?D@|@B`@BTDTJb@\\|@b@l@PVJNFFJHf@`@FH@@PN^b@ZNp@p@NLf@d@NJLJb@ZFFFDb@`@b@^pB`B|@j@XPx@^BAD?VJb@N|At@B@bD~AnEhB?A@??A?A@??A@?@??A@??@@?@??@@??@?@@??@?@?@@??@?@?@A?j@d@FFnAjAjCfCj@p@pC|C`AbAtArA`BtAjChBh@^HF^V`BfAJJvAfArBrB~ArBNPl@v@^f@pAzAn@p@j@Zh@E@??A?A@A?A@??A@A@A@A@?@A@?@?@?@?@?@@@??@@?@@@@?@@??@@@?@?@@??@?@@@?@?@?@?@?B?@@@A@?@?@^z@nAz@fAx@JLxAfBp@n@bBfBPRd@j@rAbB\\`@bBhBj@l@J@LLTVzBhClCbCxBfBd@\\HRX^LLt@h@dAb@lA`@n@PtAPpBDJA`E[pFYJ?bGHlB@pDCtAOLE|Aq@l@WdEmBtLiFVMbDyAnCqAlB_Ax@]DAvAs@lB{@tFeCHEJEl@Wz@[tBc@pCi@jCi@JCvBe@");
        List<LatLng> carlow4 = PolyUtil.decode("_`t`Iz_{g@qD`DoArAu@z@s@~@w@hAsApB{@vAWh@OXgBdDa@t@Yh@a@x@aAnBwApC}CbGqJxQqBrDg@|@e@~@e@z@]j@W^[b@a@j@Y^sCpDy@dA}I`LWZoA~AgDhE}BtCsBjCoA`BUX}ApBiAxAa@h@SXqBjC]b@]b@IH_AnAyAnB}@hAk@t@MPu@bAwGxIsAdBcApAeAtA]^qDzEmB`Ck@t@gAtAGJSVe@l@q@z@[b@]b@w@dAKJQVk@t@cArAi@r@m@z@c@l@UZQXUd@OZKTEJM\\GPMXGRUn@Sp@Sn@]dAq@|B[`Ak@lBe@vA[dAgApDc@vAWv@Wx@u@bCq@zB_@nASl@a@fAM\\Uh@{@hBa@v@]h@a@h@_@b@w@v@A@_A~@u@r@k@h@i@h@k@j@gAfAm@l@WTeAhAk@p@[^g@l@U^W`@QZGNINc@z@aAtBs@`Bc@bAi@|A_@dAKPOl@_@vAS|@WfAOt@gDvRWjBIz@KjAEl@C^IrACr@CbA?`A?bA@t@Dz@D~@Bb@Dn@?D");
        List<LatLng> carlow5 = PolyUtil.decode("yr{`I`ksi@kAiAw@aAmA_Bm@_Ae@{@g@}@_@_Ac@cAKU[_A[_AMa@a@}AAGo@yCwEcYW}Aq@mEw@}EmA}GKi@WiAU}@Qe@c@wAi@_Bi@sA_A{Bm@eA[e@yAoB_@]gB_BuBcBmCkBs@k@yAuA_GcHuM{OCC");
        List<LatLng> carlow6 = PolyUtil.decode("}_kaIrxzh@M\\a@hAa@rAk@dCa@fCGd@QtAUtCI`AALCPw@bGObAQnAq@bEi@bD?@[lA?@Wx@Sn@m@nBCJUvAMn@Kh@CPMp@ERUx@IV[fAERAHAHAJCTA\\@jA@h@@R@\\Ah@GfAW~DE~@Gz@KdAKz@WnBC^ATArAZbD@\\?V?^?d@KbFDjF?h@?HDt@Hz@ZbD?~@?h@A\\Cd@Gn@It@]rBSp@CLKd@Mv@[|AUtAIX?BWx@Un@]v@Q\\SXy@|@SXKPS^O^KZIZCNCNATCX?TIjDGfAAP");
        List<LatLng> carlow7 = PolyUtil.decode("gfoaItomi@NyAXqCFk@ZiDDWBUZyARuAFe@TmAN{@x@oEX}Aj@{CH_@RkAVqAd@eCN{@Jo@j@cDHc@d@uCDk@LyABSB]J_C@S@[R}DFmADiC@YCqB?KA_@GwAKyB");
        List<LatLng> carlow8 = PolyUtil.decode("cbraIdr_i@Pf@tAlDvCvH|A`ExBzFrHfSb@fAhApD@F?FtBjGBHbBnETx@\\hA\\vAj@fCTfAp@~CX`BxAvJ");
        List<LatLng> carlow9 = PolyUtil.decode("qjr`Ibrni@s@YuAm@}@c@[U]WaA_Ae@c@aCuAiCeBwDyCkCcCWU_@]i@e@GCC?C?G?e@FyC?k@D}BBM@yB@Q?s@@aCA?@?@?@A??@?@?@?@A??@?@A??@A??@A?A?A?A?A??AA??AA??A?AA??A?A?A?AA??A?A{FPKIAiF");
        List<LatLng> carlow10 = PolyUtil.decode("gcoaIhqdi@`@tBd@jCn@`Cr@vAVn@hAhCNb@d@~ALt@V`A`@hC@RLhCDn@B|@HxDt@fPB^H`B@^Bt@B~@@?@??@@??@@??@?@@??@?@?@?@?@?@?@?@?@?@A??@A@?@A?DbA@Rj@zH?J?DADCJGX");
        List<LatLng> carlow11 = PolyUtil.decode("mgoaIdjci@IcAGo@Q}BEw@AAE_ACs@@a@FsB@OLgBHqAD}@?Q?OEaBa@wES_CMmAOqAMm@[{AOu@S{@Kk@Mo@Gk@Ea@Ce@Ai@Ak@BeAf@mIXmEHqB?iB?IAgAAGS}C_@iFC_@A]BmB@}@DeAFkA?Ad@oHHiADk@TmDDo@NqAFi@L{@TuAXoANi@VaAb@gAv@gB@CpDiGtAiCN[bCsE|AeCbAgBtAmBFIlBcCp@{@tA_Bb@i@JOT_@Ta@Vg@P[x@sBN_@L]La@j@}BDOJg@F[RkAl@}Cb@eCv@kFBMx@yGh@wEj@wF`@mE\\iDd@eGToCXoCF_L?q@?IBmK?iC?}CA}BEqAQaCM_BM{@Ku@Ge@AKIc@AGEQOs@GQa@_Bo@eBiA_C_BcDS_@qC}EcAuAsDsEeAmB");
        List<LatLng> carlow12 = PolyUtil.decode("oipaIzsdi@MB]HWH");
        List<LatLng> carlow13 = PolyUtil.decode("cspaIffhi@@A?C?A@A?A@C?A@A?AA]A_@?[A}@A}@?g@@kA@aB?}@?q@@oB@{A?oD@e@BwE@eA@mAFoH?q@@m@?e@Ds@@a@Dq@HcALyAPiCLoBT}CJmAPcCDk@Be@Fs@B]BOBKBOBKNYNS");
        List<LatLng> carlow14 = PolyUtil.decode("{rgaI`kch@f@bAFJb@p@r@fAz@pAdB|CFLl@nAbAfCt@`BfArBb@x@f@`Ap@nAz@|AdApBp@nA^p@~@nBr@tAJRJNFHHHPRp@p@jAlAhE~DfAt@jA|@NJb@X`@Vn@Zj@\\TLF@JHFDZXDB|@fALR^p@r@hAHLHLHJPTp@r@HJZZd@\\XTTPj@^FBv@b@b@Rj@Rd@NNDRFt@N^FN@PHj@Dj@B`@@V?VATARAj@IZGd@Ib@KZIXIx@[ZMrCeAXMbAa@h@Wd@SXQl@[|@g@~@i@x@e@|BmAVOf@WnDqBtAq@pAi@rAk@jAa@`@OXMNGtAw@hDmBr@a@^Qz@c@p@[TMBAPINEBCLCJCf@IDAv@Iv@G|@ED?`@A\\AxA@\\?T?VA\\AXCTCRCD?LCZGVGtBi@vAa@tAc@h@Mh@E`@Cf@ATATCJCRILGRMj@_@l@]^S`@Qt@U^Kl@MjBUp@MXI");
        List<LatLng> carlow15 = PolyUtil.decode("ek|aIzvzg@l@?dA?R?TC@?`AMvAYLCLAz@BlBJb@DVDh@Jv@NLBZ@t@@xABL?|A@dBAhA@^@N?JBL@JBRHTHd@Tz@f@`BnAVP@@jCnAzBbA`BbAbA|@FDXVnDxBp@XVH\\FVD`@Dz@FxAFvBKPDb@N|@b@l@VrA`@JDNJXXp@x@Z`@FHr@nAd@z@LTHLVXTVl@j@p@j@h@`@tBdBVRxCdC|CjCr@h@t@l@h@`@NHVLZHD@TBPBV@R?N?@?RAHEDABCDCFGHM@C@CBENo@BI@CNu@BUBMHc@^iBVwALm@t@aEf@mCDSTu@dB}E^_Ad@uAl@cBRi@Pa@JQ@C^o@R[@CLOJM@AFINMPORQv@k@XUVSr@q@BCDE@AB?BAREZBT@XAB?BABA@A@?@ADCFEDIXc@^q@j@cAFQ");
        List<LatLng> carlow16 = PolyUtil.decode("cioaI~hni@CXQrCEz@a@zGC`@]xFEr@G~@KhBG|@Cb@GvAEx@ARK|Ai@rHGd@CTO|AUtBm@rFCNGj@C^Gh@Gt@[tEMzAE|@CnB@vAEnAEfAIr@IlAKz@Kr@Qx@Ol@Qj@Q`@c@fA[`AOb@[dAY|@Up@O\\i@jAc@dAS`@Qd@CF]x@IN");


        List<LatLng> carlow1333= PolyUtil.decode(String.valueOf("i~oaIhx`h@a@~FBpAFZNX|@rAzApD~@zALNVV|@n@^RXn@xBj@~Bf@fBd@vBVbALt@Dr@@bA@fCNjLrUJnFBjCGxDGjCQhE[nLMfDGdAy@`HmBfPaAdHyCtRq@xCo@rBWfAa@vBkBbNyB|Oy@xGi@~D]nB[nBMvACd@Dt@t@jEd@hCb@xEXbDLdAfAlG~A`HHp@"));
        List<LatLng> carlow213= PolyUtil.decode("\"i~oaIhx`h@a@~FBpAFZNX|@rAzApD~@zALNVV|@n@\\\\^RXn@xBj@~Bf@fBd@vBVbALt@Dr@@bA@fCNjL\\\\rUJnFBjCGxDGjCQhE[nLMfDGdAy@`HmBfPaAdHyCtRq@xCo@rBWfAa@vBkBbNyB|Oy@xGi@~D]nB[nBMvACd@Dt@t@jEd@hCb@xEXbDLdAfAlG~A`HHp@\"");
        List<LatLng> carlo12=PolyUtil.decode("i~oaIhx`h@a@~FBpAFZNX|@rAzApD~@zALNVV|@n@\\^RXn@xBj@~Bf@fBd@vBVbALt@Dr@@bA@fCNjL\\rUJnFBjCGxDGjCQhE[nLMfDGdAy@`HmBfPaAdHyCtRq@xCo@rBWfAa@vBkBbNyB|Oy@xGi@~D]nB[nBMvACd@Dt@t@jEd@hCb@xEXbDLdAfAlG~A`HHp@");
        listOLists.add(carlow1);
        listOLists.add(carlow2);
        listOLists.add(carlow3);
        listOLists.add(carlow4);
        listOLists.add(carlow5);
        listOLists.add(carlow6);
        listOLists.add(carlow7);
        listOLists.add(carlow8);
        listOLists.add(carlow9);
        listOLists.add(carlow10);
        listOLists.add(carlow11);
        listOLists.add(carlow12);
        listOLists.add(carlow13);
        listOLists.add(carlow14);
        listOLists.add(carlow15);
        listOLists.add(carlow16);


    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service started");
        boolean startedFromNotification = intent.getBooleanExtra(EXTRA_STARTED_FROM_NOTIFICATION,
                false);

        // We got here because the user decided to remove location updates from the notification.
        if (startedFromNotification) {
            removeLocationUpdates();
            stopSelf();
        }
        // Tells the system to not try to recreate the service after it has been killed.
        return START_NOT_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) comes to the foreground
        // and binds with this service. The service should cease to be a foreground service
        // when that happens.
        Log.i(TAG, "in onBind()");
        stopForeground(true);
        mChangingConfiguration = false;
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        // Called when a client (MainActivity in case of this sample) returns to the foreground
        // and binds once again with this service. The service should cease to be a foreground
        // service when that happens.
        Log.i(TAG, "in onRebind()");
        stopForeground(true);
        mChangingConfiguration = false;
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.i(TAG, "Last client unbound from service");

        // Called when the last client (MainActivity in case of this sample) unbinds from this
        // service. If this method is called due to a configuration change in MainActivity, we
        // do nothing. Otherwise, we make this service a foreground service.
        if (!mChangingConfiguration && Utils.requestingLocationUpdates(this)) {
            Log.i(TAG, "Starting foreground service");

            startForeground(NOTIFICATION_ID, getNotification());
        }
        return true; // Ensures onRebind() is called when a client re-binds.
    }

    @Override
    public void onDestroy() {
        mServiceHandler.removeCallbacksAndMessages(null);
    }

    /**
     * Makes a request for location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public void requestLocationUpdates() {
        Log.i(TAG, "Requesting location updates");
        Utils.setRequestingLocationUpdates(this, true);
        startService(new Intent(getApplicationContext(), LocationUpdatesService.class));
        try {
            mFusedLocationClient.requestLocationUpdates(mLocationRequest,
                    mLocationCallback, Looper.myLooper());
        } catch (SecurityException unlikely) {
            Utils.setRequestingLocationUpdates(this, false);
            Log.e(TAG, "Lost location permission. Could not request updates. " + unlikely);
        }
    }

    /**
     * Removes location updates. Note that in this sample we merely log the
     * {@link SecurityException}.
     */
    public void removeLocationUpdates() {
        Log.i(TAG, "Removing location updates");
        try {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback);
            Utils.setRequestingLocationUpdates(this, false);
            stopSelf();
        } catch (SecurityException unlikely) {
            Utils.setRequestingLocationUpdates(this, true);
            Log.e(TAG, "Lost location permission. Could not remove updates. " + unlikely);
        }
    }

    /**
     * Returns the {@link NotificationCompat} used as part of the foreground service.
     */
    private Notification getNotification() {
        Intent intent = new Intent(this, LocationUpdatesService.class);

        CharSequence text = Utils.getLocationText(mLocation);

        // Extra to help us figure out if we arrived in onStartCommand via the notification or not.
        intent.putExtra(EXTRA_STARTED_FROM_NOTIFICATION, true);

        // The PendingIntent that leads to a call to onStartCommand() in this service.
        PendingIntent servicePendingIntent = PendingIntent.getService(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // The PendingIntent to launch activity.
        PendingIntent activityPendingIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .addAction(R.drawable.ic_launch, getString(R.string.launch_activity),
                        activityPendingIntent)
                .addAction(R.drawable.ic_cancel, getString(R.string.remove_location_updates),
                        servicePendingIntent)
                .setContentText(text)
                .setContentTitle(Utils.getLocationTitle(this))
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(text)
                .setWhen(System.currentTimeMillis());

        // Set the Channel ID for Android O.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID); // Channel ID
        }

        return builder.build();
    }

    private void getLastLocation() {
        try {
            mFusedLocationClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if (task.isSuccessful() && task.getResult() != null) {
                                mLocation = task.getResult();
                            } else {
                                Log.w(TAG, "Failed to get location.");
                            }
                        }
                    });
        } catch (SecurityException unlikely) {
            Log.e(TAG, "Lost location permission." + unlikely);
        }
    }

//    @Override
//    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
//        View view =   inflater.inflate(R.layout.activity_main,container,false);
//        speedVan = (ImageView) view.findViewById(R.id.speedvan);
//        return view;
//    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void onNewLocation(Location location) {
        Log.i(TAG, "New location: " + location);
        LatLng mypoint = new LatLng(location.getLatitude(),location.getLongitude());

        //check the county here

        listOLists.stream().parallel().forEach(obj ->{
            boolean isonPath = PolyUtil.isLocationOnPath(mypoint,obj,true,100);
            if(isonPath){
                Log.i(TAG, "my location is on path is: " + isonPath + " mylocation is : "+mypoint);
                Intent intent = new Intent(ACTION_BROADCAST);
                intent.putExtra(EXTRA_LOCATION, location);
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
            }
        });
        mLocation = location;


        // Notify anyone listening for broadcasts about the new location.
//        Intent intent = new Intent(ACTION_BROADCAST);
//        intent.putExtra(EXTRA_LOCATION, location);
//        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);

        // Update notification content if running as a foreground service.
        if (serviceIsRunningInForeground(this)) {
            mNotificationManager.notify(NOTIFICATION_ID, getNotification());
        }
    }

    /**
     * Sets the location request parameters.
     */
    private void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    /**
     * Class used for the client Binder.  Since this service runs in the same process as its
     * clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
        LocationUpdatesService getService() {
            return LocationUpdatesService.this;
        }
    }

    /**
     * Returns true if this is a foreground service.
     *
     * @param context The {@link Context}.
     */
    public boolean serviceIsRunningInForeground(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(
                Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(
                Integer.MAX_VALUE)) {
            if (getClass().getName().equals(service.service.getClassName())) {
                if (service.foreground) {
                    return true;
                }
            }
        }
        return false;
    }
}
