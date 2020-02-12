/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.vehiclebussample.fragments;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.micronet.vehiclebussample.BuildConfig;
import com.micronet.vehiclebussample.R;

import com.micronet.vehiclebussample.receivers.DeviceStateReceiver;

public class AboutFragment extends Fragment {

    private static final String TAG = "SHAboutFragment";
    private View rootView;

    public AboutFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_about, container, false);
        TextView txtAbout = rootView.findViewById(R.id.txtAppInfo);
        txtAbout.setText(String.format("Vehicle Bus Library Sample App v%s\n" +
                "Copyright © 2020 Micronet Inc.", BuildConfig.VERSION_NAME));
        txtAbout.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);

        updateInfoText();
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();

        Context context = getContext();
        if (context != null) {
            LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, DeviceStateReceiver.getLocalIntentFilter());
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        Context context = getContext();
        if (context != null) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver);
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            goAsync();

            String action = intent.getAction();

            if (action != null) {
                switch (action) {
                    case DeviceStateReceiver.portsAttachedAction:
                        Handler handler = new Handler();
                        handler.postDelayed(updateTextRunnable, 2000);
                        Log.d(TAG, "Ports attached event received");
                        break;
                    case DeviceStateReceiver.portsDetachedAction:
                        updateInfoText();
                        Log.d(TAG, "Ports detached event received");
                        break;
                }
            }
        }
    };

    Runnable updateTextRunnable = new Runnable() {
        @Override
        public void run() {
            updateInfoText();
        }
    };

    @SuppressLint({"HardwareIds", "MissingPermission"})
    public void updateInfoText() {
        // Remove the second view so we can update it
        LinearLayout linearLayout = rootView.findViewById(R.id.aboutLinearLayout);
        if (linearLayout.getChildCount() > 2) {
            linearLayout.removeViewAt(2);
        }

        TableLayout table = new TableLayout(getContext());
        table.setStretchAllColumns(true);
        table.addView(getTableRow("Vehicle Library Version: ", "v" + com.micronet.canbus.Info.VERSION));
        table.addView(getTableRow("OS Version: ", Build.DISPLAY.split(" ")[3]));
        table.addView(getTableRow("Build Type: ", Build.TYPE));
        table.addView(getTableRow("Android Build Version: ", VERSION.RELEASE));
        table.addView(getTableRow("Device Model: ", Build.MODEL));
        table.addView(getTableRow("Serial: ", Build.getSerial()));
        linearLayout.addView(table);

        Log.d(TAG, "Updated text on info tab");
    }

    @NonNull
    private TableRow getTableRow(String paramName, String paramValue) {
        TableRow row = new TableRow(getContext());

        LinearLayout.LayoutParams param = new LinearLayout.LayoutParams(
                LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT,
                0.0f
        );

        row.setGravity(Gravity.CENTER_HORIZONTAL);
        row.setLayoutParams(param);

        TextView tmp = new TextView(getContext());
        tmp.setText(paramName);
        tmp.setTextColor(Color.BLACK);
        tmp.setGravity(Gravity.CENTER);
        row.addView(tmp);

        tmp = new TextView(getContext());
        tmp.setText(paramValue);
        tmp.setTextColor(Color.BLACK);
        tmp.setGravity(Gravity.CENTER);
        row.addView(tmp);
        return row;
    }
}
