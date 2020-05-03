package com.google.android.gms.location.sample.locationupdatesforegroundservice;

import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.PolyUtil;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class DatabaseHelper extends SQLiteOpenHelper {
    public static String DATABASE_NAME ="locationInfo.db";
    private static final String TAG = DatabaseHelper.class.getSimpleName();

    public DatabaseHelper(@Nullable Context context){
        super(context, DATABASE_NAME, null, 1);
        SQLiteDatabase db = this.getWritableDatabase();
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS locationInfo (id INTEGER PRIMARY KEY AUTOINCREMENT, county VARCHAR(30) NOT NULL, polyline VARCHAR(500))");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS locationInfo");
        onCreate(db);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    void readFileAndInsertData(Context context) {
        InputStream is = context.getResources().openRawResource(R.raw.sqlinserts);
        SQLiteDatabase db = this.getWritableDatabase();
        long numRows = DatabaseUtils.longForQuery(db, "SELECT COUNT(*) FROM locationInfo", null);
        if (numRows < 500) {
            Log.i(TAG,"Inserting data in the database");
            try {
                String s = IOUtils.toString(is);
                String[] lines = s.split(Objects.requireNonNull(System.getProperty("line.separator")));
                for (String line : lines) {
                    db.execSQL(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            IOUtils.closeQuietly(is);
        }
        else
        {
            Log.i(TAG,"Database already has the data");
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public Map<String, ArrayList<List<LatLng>>> getMapFromDatabase(){
        String selectQuery = "SELECT  * FROM locationInfo";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);
        List <String> counties = Arrays.asList("carlow", "cavan","clare","cork","donegal","dublin","galway","kerry","kildare","kilkenny","laois","leitrim","limerick","longford","louth","mayo","meath","monaghan","offaly","roscommon","sligo","tipperary","waterford","westmeath","wexford","wicklow");
        Map<String, ArrayList<List<LatLng>>> mapWithAllLocations = new HashMap<String, ArrayList<List<LatLng>>>();
        String county;
        String polyline;
        if (cursor.moveToFirst()) {
            ArrayList<List<LatLng>> locations = new ArrayList<>();
            do {
                county = cursor.getString(1);
                polyline = cursor.getString(2);
                for(String countyFromList : counties){
                    if(county.equalsIgnoreCase(countyFromList)){
                        try{
                            List<LatLng> decodedPolyline = PolyUtil.decode(StringEscapeUtils.unescapeJava(polyline));
                            mapWithAllLocations.computeIfAbsent(countyFromList, k ->new ArrayList<>()).add(decodedPolyline);
                        }
                          catch (Exception e){
                                Log.e("TAG","polyline cannot be decoded");
                          }
                    }
                }
//                List<LatLng> temp =
            } while (cursor.moveToNext());
        }
        cursor.close();
        return mapWithAllLocations;
    }
}
