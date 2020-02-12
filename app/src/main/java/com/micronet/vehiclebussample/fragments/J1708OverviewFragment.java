/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.vehiclebussample.fragments;

import static java.lang.Thread.sleep;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import com.micronet.vehiclebussample.J1708FramesViewModel;
import com.micronet.vehiclebussample.J1708Test;
import com.micronet.vehiclebussample.R;
import java.util.Calendar;
import java.util.Date;

public class J1708OverviewFragment extends Fragment {

    private final String TAG = "J1708OverviewFragment";
    private View rootView;

    private Date LastCreated;
    private Date LastClosed;

    private Thread updateUIThread;

    private J1708Test j1708Test;
    private TextView txtInterfaceClsTimeJ1708;
    private TextView txtInterfaceOpenTimeJ1708;
    private TextView txtTxSpeedJ1708;

    private TextView textViewFramesRx;
    private TextView textViewFramesTx;

    private J1708FramesViewModel j1708FramesViewModel;

    // Socket dependent UI
    private Button btnTransmitJ1708;
    private ToggleButton swCycleTransmitJ1708;
    private SeekBar seekBarJ1708Send;

    private Button openJ1708;
    private Button closeJ1708;

    private int mDockState = -1;
    private boolean reopenJ1708OnTtyAttachEvent = false;

    private StartJ1708Task startJ1708Task;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        j1708Test = new J1708Test();
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        IntentFilter filters = new IntentFilter();
        filters.addAction("com.micronet.smarttabsmarthubsampleapp.dockevent");
        filters.addAction("com.micronet.smarttabsmarthubsampleapp.portsattached");
        filters.addAction("com.micronet.smarttabsmarthubsampleapp.portsdetached");

        Context context = getContext();
        if (context != null){
            LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, filters);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");

        Context context = getContext();
        if (context != null) {
            LocalBroadcastManager.getInstance(context).unregisterReceiver(broadcastReceiver);
        }
    }

    private void setStateSocketDependentUI() {
        boolean open = j1708Test.isPortSocketOpen();
        btnTransmitJ1708.setEnabled(open);
        swCycleTransmitJ1708.setEnabled(open);
        seekBarJ1708Send.setEnabled(open);
    }

    private void setDockStateDependentUI(){
        boolean uiElementEnabled = true;
        if (mDockState == Intent.EXTRA_DOCK_STATE_UNDOCKED){
            uiElementEnabled = false;
        }
        openJ1708.setEnabled(uiElementEnabled);
        closeJ1708.setEnabled(uiElementEnabled);
    }

    private void updateInterfaceStatusUI(String status) {
        final TextView txtInterfaceStatus = rootView.findViewById(R.id.textJ1708InterfaceStatus);
        if(status != null) {
            txtInterfaceStatus.setText(status);
            txtInterfaceStatus.setBackgroundColor(Color.YELLOW);
        } else if(j1708Test.isJ1708InterfaceOpen()) {
            txtInterfaceStatus.setText(getString(R.string.open));
            txtInterfaceStatus.setBackgroundColor(Color.GREEN);
        } else { // closed
            txtInterfaceStatus.setText(getString(R.string.closed));
            txtInterfaceStatus.setBackgroundColor(Color.RED);
        }

        final TextView txtSocketStatus = rootView.findViewById(R.id.textJ1708SocketStatus);
        if(status != null) {
            txtSocketStatus.setText(status);
            txtSocketStatus.setBackgroundColor(Color.YELLOW);
        } else if(j1708Test.isPortSocketOpen()) {
            txtSocketStatus.setText(getString(R.string.open));
            txtSocketStatus.setBackgroundColor(Color.GREEN);
        } else { // closed
            txtSocketStatus.setText(getString(R.string.closed));
            txtSocketStatus.setBackgroundColor(Color.RED);
        }
    }

    private void updateInterfaceStatusUI() {
        updateInterfaceStatusUI(null);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart");
    }

    private void openJ1708Interface(){
        j1708Test.setRemoveJ1708InterfaceState(false);

        if (startJ1708Task == null || startJ1708Task.getStatus() != AsyncTask.Status.RUNNING) {
            startJ1708Task = new StartJ1708Task();
            startJ1708Task.execute();
        }
    }

    private void closeJ1708Interface(){
        j1708Test.setRemoveJ1708InterfaceState(true);
        clearUIFrameCounts();

        if (startJ1708Task == null || startJ1708Task.getStatus() != AsyncTask.Status.RUNNING) {
            startJ1708Task = new StartJ1708Task();
            startJ1708Task.execute();
        }
    }

    private class StartJ1708Task extends AsyncTask<Void, String, Void> {

        private StartJ1708Task() {

        }

        @Override
        protected Void doInBackground(Void... params) {
            LastClosed = Calendar.getInstance().getTime();
            if(j1708Test.isJ1708InterfaceOpen() || j1708Test.isPortSocketOpen()) {
                publishProgress("Closing interface, please wait...");
                j1708Test.closeJ1708Interface();
                publishProgress("Closing socket, please wait...");
                j1708Test.closeJ1708Socket();
                return null;
            }

            publishProgress("Opening, please wait...");
            int ret = j1708Test.createJ1708Interface();
            if (ret == 0) {
                LastCreated = Calendar.getInstance().getTime();
            }
            else{
                publishProgress("Closing interface, please wait...");
                j1708Test.closeJ1708Interface();
                publishProgress("Closing socket, please wait...");
                j1708Test.closeJ1708Socket();
                publishProgress("failed");
            }
            return null;
        }

        protected void onProgressUpdate(String... params) {
            updateInterfaceStatusUI(params[0]);
            setStateSocketDependentUI();
            setDockStateDependentUI();
        }

        protected void onPostExecute(Void result) {
            updateInterfaceTime();
            startUpdateUIThread();
            updateInterfaceStatusUI();
            setStateSocketDependentUI();
            setDockStateDependentUI();
        }
    }

    private void updateRxFramesViewModel() {
        if (j1708FramesViewModel.j1708FramesRxStr.getValue() == null){
            j1708FramesViewModel.j1708FramesRxStr.setValue("");
        }
        j1708FramesViewModel.j1708BytesRx.setValue(j1708Test.getPortJ1708RxByteCount());
        j1708FramesViewModel.j1708FramesRx.setValue(j1708Test.getPortJ1708RxFrameCount());
        j1708FramesViewModel.j1708FramesRxStr.setValue(j1708FramesViewModel.j1708FramesRxStr.getValue() + j1708Test.j1708Data);
        j1708Test.j1708Data.setLength(0);
    }

    private void updateTxFramesViewModel(){
        if (j1708FramesViewModel.j1708FramesTx.getValue() == null){
            j1708FramesViewModel.j1708FramesTx.setValue(0);
        }
        j1708FramesViewModel.j1708BytesTx.setValue(j1708Test.getPortJ1708TxByteCount());
        j1708FramesViewModel.j1708FramesTx.setValue(j1708Test.getPortJ1708TxFrameCount());
    }

    private static int lastUIFramesUpdatedCountRx = 0;
    private static int lastUIFramesUpdatedCountTx = 0;
    private void updateCountUI() {

        if (j1708Test != null){
            if (j1708Test.getPortJ1708RxFrameCount() != lastUIFramesUpdatedCountRx){
                updateRxFramesViewModel();
                lastUIFramesUpdatedCountRx = j1708Test.getPortJ1708RxFrameCount();
            }

            if (j1708Test.getPortJ1708TxFrameCount() != lastUIFramesUpdatedCountTx){
                updateTxFramesViewModel();
                lastUIFramesUpdatedCountTx = j1708Test.getPortJ1708TxFrameCount();
            }
        }
    }

    private void updateInterfaceTime() {
        String closedDate = " None ";
        String createdDate = " None ";
        if(LastClosed != null){
            closedDate = LastClosed.toString();
        }
        if(LastCreated != null){
            createdDate = LastCreated.toString();
        }

        txtInterfaceOpenTimeJ1708.setText(createdDate);
        txtInterfaceClsTimeJ1708.setText(closedDate);
    }


    private void startUpdateUIThread() {
        if (updateUIThread == null) {
            updateUIThread = new Thread(new Runnable() {
                @SuppressWarnings("InfiniteLoopStatement")
                @Override
                public void run() {
                    while (true) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                updateCountUI();
                            }
                        });
                        try {
                            sleep(500);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }

        if (!updateUIThread.isAlive()) {
            updateUIThread.start();
        }
    }

    private void clearUIFrameCounts(){
        j1708FramesViewModel.j1708FramesTx.setValue(0);
        j1708FramesViewModel.j1708BytesTx.setValue(0);
        j1708FramesViewModel.j1708FramesRx.setValue(0);
        j1708FramesViewModel.j1708BytesRx.setValue(0);
        j1708FramesViewModel.j1708FramesRxStr.setValue("");
    }

    private void subscribeJ1708Frames(){
        j1708FramesViewModel.j1708FramesRx.observe(getActivity(), new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer j1708FramesRx) {
                if (j1708FramesRx != null){
                    String s1 = j1708FramesViewModel.j1708FramesRx.getValue() + " Frames / " + j1708FramesViewModel.j1708BytesRx.getValue() + " Bytes";
                    textViewFramesRx.setText(s1);
                    if (j1708FramesViewModel.j1708FramesRx.getValue() == 0){
                        textViewFramesRx.setBackgroundColor(Color.WHITE);
                    }
                    else{
                        textViewFramesRx.setBackgroundColor(Color.GREEN);
                    }
                }
            }
        });

        j1708FramesViewModel.j1708FramesTx.observe(getActivity(), new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer j1708FramesTx) {
                if (j1708FramesTx != null){
                    String s2 = "Tx: " + j1708FramesViewModel.j1708FramesTx.getValue() + " Frames / " + j1708FramesViewModel.j1708BytesTx.getValue() + " Bytes";
                    textViewFramesTx.setText(s2);
                    if (j1708FramesViewModel.j1708FramesTx.getValue() == 0) {
                        textViewFramesTx.setBackgroundColor(Color.WHITE);
                    }
                    else{
                        textViewFramesTx.setBackgroundColor(Color.GREEN);
                    }
                }
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        Log.d(TAG, "onCreateView Start");
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.fragment_j1708_overview, container, false);

        j1708FramesViewModel = ViewModelProviders.of(getActivity()).get(J1708FramesViewModel.class);

        textViewFramesRx = rootView.findViewById(R.id.textViewJ1708FramesRx);
        textViewFramesTx = rootView.findViewById(R.id.textViewJ1708FramesTx);

        openJ1708 = rootView.findViewById(R.id.buttonOpenJ1708);
        closeJ1708 = rootView.findViewById(R.id.buttonCloseJ1708);
        txtInterfaceClsTimeJ1708 = rootView.findViewById(R.id.textViewJ1708ClosedTime);
        txtInterfaceOpenTimeJ1708 = rootView.findViewById(R.id.textViewJ1708CreatedTime);
        txtTxSpeedJ1708 = rootView.findViewById(R.id.textViewJ1708CurrTransmitInterval);

        btnTransmitJ1708 = rootView.findViewById(R.id.btnJ1708Send);
        seekBarJ1708Send = rootView.findViewById(R.id.seekBarJ1708SendSpeed);
        swCycleTransmitJ1708 = rootView.findViewById(R.id.swJ1708CycleTransmit);

        seekBarJ1708Send.setProgress(j1708Test.getJ1708IntervalDelay());
        btnTransmitJ1708.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                j1708Test.sendJ1708Port();
            }
        });

        openJ1708.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openJ1708Interface();
            }
        });

        closeJ1708.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeJ1708Interface();
            }
        });

        swCycleTransmitJ1708.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                j1708Test.setAutoSendJ1708Port(swCycleTransmitJ1708.isChecked());
                j1708Test.sendJ1708Port();
            }
        });

        txtTxSpeedJ1708.setText(String.valueOf(seekBarJ1708Send.getProgress()) + "ms");

        seekBarJ1708Send.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    j1708Test.setJ1708IntervalDelay(progress + 200);
                    String progressStr = String.valueOf(progress + 200) + "ms";
                    txtTxSpeedJ1708.setText(progressStr);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        //txtCanTxSpeedCan1.setText(canTest.getJ1939IntervalDelay() + "ms");
        updateInterfaceTime();
        updateInterfaceStatusUI();
        setStateSocketDependentUI();
        setDockStateDependentUI();
        subscribeJ1708Frames();
        Log.d(TAG, "onCreateView end");
        return rootView;
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            goAsync();

            String action = intent.getAction();

            if (action != null) {
                switch (action) {
                    case "com.micronet.smarttabsmarthubsampleapp.dockevent":
                        mDockState = intent.getIntExtra(Intent.EXTRA_DOCK_STATE, -1);
                        updateCradleIgnState();
                        Log.d(TAG, "Dock event received: " + mDockState);
                        break;
                    case "com.micronet.smarttabsmarthubsampleapp.portsattached":
                        if (reopenJ1708OnTtyAttachEvent){
                            Log.d(TAG, "Reopening J1708 port since the tty port attach event was received");
                            Toast.makeText(getContext().getApplicationContext(), "Reopening J1708 port since the tty port attach event was received",
                                    Toast.LENGTH_SHORT).show();
                            openJ1708Interface();
                            reopenJ1708OnTtyAttachEvent = false;
                        }
                        Log.d(TAG, "Ports attached event received");
                        break;
                    case "com.micronet.smarttabsmarthubsampleapp.portsdetached":
                        if (j1708Test.isJ1708InterfaceOpen()){
                            Log.d(TAG, "closing J1708 port since the tty port detach event was received");
                            Toast.makeText(getContext().getApplicationContext(), "closing J1708 port since the tty port detach event was received",
                                    Toast.LENGTH_SHORT).show();
                            closeJ1708Interface();
                            reopenJ1708OnTtyAttachEvent = true;
                        }
                        Log.d(TAG, "Ports detached event received");
                        break;
                }
            }
        }
    };

    private void updateCradleIgnState(){
        String cradleStateMsg, ignitionStateMsg;
        Log.d(TAG, "updateCradleIgnState() mDockState:" + mDockState);
        switch (mDockState) {
            case Intent.EXTRA_DOCK_STATE_UNDOCKED:
                cradleStateMsg = getString(R.string.not_in_cradle_state_text);
                ignitionStateMsg = getString(R.string.ignition_unknown_state_text);
                break;
            case Intent.EXTRA_DOCK_STATE_DESK:
            case Intent.EXTRA_DOCK_STATE_LE_DESK:
            case Intent.EXTRA_DOCK_STATE_HE_DESK:
                cradleStateMsg = getString(R.string.in_cradle_state_text);
                //ignitionStateMsg = getString(R.string.ignition_off_state_text);
                ignitionStateMsg = getString(R.string.ignition_off_state_text);
                break;
            case Intent.EXTRA_DOCK_STATE_CAR:
                cradleStateMsg = getString(R.string.in_cradle_state_text);
                ignitionStateMsg = getString(R.string.ignition_on_state_text);
                break;
            default:
                /* this state indicates un-defined docking state */
                cradleStateMsg = getString(R.string.not_in_cradle_state_text);
                ignitionStateMsg = getString(R.string.ignition_unknown_state_text);
                break;
        }

        TextView cradleStateTextView = rootView.findViewById(R.id.textViewCradleState);
        TextView ignitionStateTextView = rootView.findViewById(R.id.textViewIgnitionState);
        cradleStateTextView.setText(cradleStateMsg);
        ignitionStateTextView.setText(ignitionStateMsg);
    }
}
