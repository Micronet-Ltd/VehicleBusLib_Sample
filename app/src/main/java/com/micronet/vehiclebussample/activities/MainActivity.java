/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.vehiclebussample.activities;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import com.micronet.vehiclebussample.R;
import com.micronet.vehiclebussample.fragments.AboutFragment;
import com.micronet.vehiclebussample.fragments.Can1OverviewFragment;
import com.micronet.vehiclebussample.fragments.Can2OverviewFragment;
import com.micronet.vehiclebussample.fragments.CanBusFramesFragment;
import com.micronet.vehiclebussample.fragments.J1708FramesFragment;
import com.micronet.vehiclebussample.fragments.J1708OverviewFragment;
import com.micronet.vehiclebussample.receivers.DeviceStateReceiver;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "SmartSampleApp";
    private final int PERMISSION_CODE = 4658434;

    private static boolean portsAttached = false;
    private static int dockState = -1;

    private DeviceStateReceiver deviceStateReceiver = new DeviceStateReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_PHONE_STATE}, PERMISSION_CODE);
        } else {
            setupUserInterface();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_CODE && permissions[0].equals(Manifest.permission.READ_PHONE_STATE) && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupUserInterface();
        } else {
            Toast.makeText(this, "Permissions not granted. Closing.", Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void setupUserInterface() {
        setContentView(R.layout.activity_main);

        ViewPager viewPager = findViewById(R.id.viewpager);
        setupViewPager(viewPager);

        TabLayout tabLayout = findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(viewPager);

        // Check if tty ports are available
        UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
        if (usbManager != null) {
            HashMap<String, UsbDevice> connectedDevices = usbManager.getDeviceList();
            for (UsbDevice device : connectedDevices.values()) {
                // Check if tty ports are enumerated
                if (device.getProductId() == 773 && device.getVendorId() == 5538) {
                    portsAttached = true;
                }
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Receive any dock events or usb events
        IntentFilter filters = new IntentFilter();
        filters.addAction(Intent.ACTION_DOCK_EVENT);
        filters.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filters.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(deviceStateReceiver, filters);
    }

    @Override
    protected void onPause() {
        super.onPause();

        // Unregister receiver
        unregisterReceiver(deviceStateReceiver);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Log.d(TAG, "Configuration changed: " + newConfig.toString());
    }

    @SuppressWarnings("unused")
    public static synchronized boolean areTtyPortsAvailable() {
        return portsAttached;
    }

    public static synchronized void setTtyPortsState(boolean state) {
        portsAttached = state;
    }

    public static synchronized int getDockState() {
        return dockState;
    }

    public static synchronized void setDockState(int state) {
        dockState = state;
    }

    private void setupViewPager(ViewPager viewPager) {
        ViewPagerAdapter adapter = new ViewPagerAdapter(getSupportFragmentManager());
//        adapter.addFragment(new InputOutputsFragment(), "GPIOs");
        adapter.addFragment(new Can1OverviewFragment(), "Can1");
        adapter.addFragment(new Can2OverviewFragment(), "Can2");
        adapter.addFragment(new CanBusFramesFragment(), "Can Frames");
        adapter.addFragment(new J1708OverviewFragment(), "J1708");
        adapter.addFragment(new J1708FramesFragment(), "J1708 Frames");
        adapter.addFragment(new AboutFragment(), "Info");
        viewPager.setAdapter(adapter);
    }

    class ViewPagerAdapter extends FragmentPagerAdapter {

        private final List<Fragment> mFragmentList = new ArrayList<>();
        private final List<String> mFragmentTitleList = new ArrayList<>();

        private ViewPagerAdapter(FragmentManager manager) {
            super(manager);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return mFragmentList.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitleList.get(position);
        }

        private void addFragment(Fragment fragment, String title) {
            mFragmentList.add(fragment);
            mFragmentTitleList.add(title);
        }
    }
}
