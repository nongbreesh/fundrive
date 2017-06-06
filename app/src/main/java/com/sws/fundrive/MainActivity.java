package com.sws.fundrive;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;


public class MainActivity extends AppCompatActivity implements LocationListener {

    private FrameLayout mFlo;
    private View driveboard;
    private TextView txtspeed, txtCoins;
    private SeekBar seekBarMin;
    LocationManager locManager;
    LocationListener li;
    long init, now, time, paused, sec, sumsec;
    int coins = 0;
    TextView display;
    Handler handler;
    private Runnable updater;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mFlo.removeAllViews();
                    mFlo.addView(driveboard, LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
                    return true;
                case R.id.navigation_dashboard:
                    mFlo.removeAllViews();
                    mFlo.addView(driveboard);
                    return true;
                case R.id.navigation_notifications:
                    mFlo.removeAllViews();
                    mFlo.addView(driveboard);
                    return true;
            }
            return false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mFlo = (FrameLayout) findViewById(R.id.content);

        driveboard = inflater.inflate(R.layout.driveboard, null); //the xml code
        txtspeed = (TextView) driveboard.findViewById(R.id.txtspeed);
        txtCoins = (TextView) driveboard.findViewById(R.id.txtCoins);
        seekBarMin = (SeekBar) driveboard.findViewById(R.id.seekBarMin);
        seekBarMin.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return true;
            }
        });

        mFlo.addView(driveboard);

        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
        this.turnGPSOn(this);
        handler = new Handler();
        updater = new Runnable() {
            @Override
            public void run() {
                now = System.currentTimeMillis();
                time = now - init;
                sec = time / 1000;
                Log.d("sumsec", sumsec + "");
                seekBarMin.setProgress((int) sumsec);
                handler.postDelayed(this, 1000);
            }
        };
    }

    /**
     * Method to turn on GPS
     **/
    public void turnGPSOn(final Context context) {

        LocationManager lm = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);
        this.updateSpeed(null);

        if (!gps_enabled && !network_enabled) {
            // notify user
            AlertDialog.Builder dialog = new AlertDialog.Builder(context);
            dialog.setMessage(context.getResources().getString(R.string.gps_network_not_enabled));
            dialog.setPositiveButton(context.getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    // TODO Auto-generated method stub
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    context.startActivity(myIntent);
                    //get gps
                }
            });
            dialog.setNegativeButton(context.getString(R.string.Cancel), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {
                    // TODO Auto-generated method stub

                }
            });
            dialog.show();
        }

    }

    public void finish() {
        super.finish();
        System.exit(0);
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = System.currentTimeMillis();
    }

    @Override
    protected void onResume() {
        super.onResume();
        init += System.currentTimeMillis() - paused;
    }

    private void updateSpeed(Location location) {

        // TODO Auto-generated method stub
        int nCurrentSpeed = 0;

        if (location != null) {
            if (location.hasSpeed()) {
                nCurrentSpeed = (int) ((location.getSpeed() * 3600) / 1000);
            }
        }
        String strUnits = "km/h";
        if (nCurrentSpeed >= 40 && nCurrentSpeed <= 90) {
            init = System.currentTimeMillis();
            handler.post(updater);
            sumsec += sec;

        } else {
            paused = System.currentTimeMillis();
        }


        if ((sumsec / 60) >= 10) {
            coins = coins + 3;
            txtCoins.setText("+ " + coins + " Coins");
            sumsec = 0;
        }

        this.txtspeed.setText(nCurrentSpeed + " " + strUnits);
    }


    @Override
    public void onLocationChanged(Location location) {
        // TODO Auto-generated method stub
        if (location != null) {
            this.updateSpeed(location);
        }
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {

    }

    @Override
    public void onProviderEnabled(String provider) {

    }

    @Override
    public void onProviderDisabled(String provider) {

    }


}



