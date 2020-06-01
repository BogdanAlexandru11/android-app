package com.google.android.gms.location.sample.locationupdatesforegroundservice;

import java.util.ArrayList;
import java.util.List;

interface BooleanListener {
    public void OnMyBooleanChanged();
}

public class BooleanVariable {
    public static boolean myBoolean;
    public static List<BooleanListener> listeners = new ArrayList<BooleanListener>();

    public boolean getMyBoolean() { return myBoolean; }

    public void setMyBoolean(boolean value) {
        myBoolean = value;

        for (BooleanListener l : listeners) {
            l.OnMyBooleanChanged();
        }
    }

    public static void addMyBooleanListener(BooleanListener l) {
        listeners.add(l);
    }
}