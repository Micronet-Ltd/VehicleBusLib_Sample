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
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import com.micronet.canbus.CanbusException;
import com.micronet.vehiclebussample.CanTest;
import com.micronet.vehiclebussample.R;
import com.micronet.vehiclebussample.CanFramesViewModel;
import com.micronet.vehiclebussample.activities.MainActivity;
import com.micronet.vehiclebussample.receivers.DeviceStateReceiver;

import java.util.Calendar;
import java.util.Date;

public class Can1OverviewFragment extends Fragment {

    private final String TAG = "Can1OverviewFragment";
    private View rootView;

    private Date LastCreated;
    private Date LastClosed;

    private final String LABEL_10K = "10K";
    private final String LABEL_20K = "20K";
    private final String LABEL_50K = "50K";
    private final String LABEL_100K = "100K";
    private final String LABEL_125K = "125K";
    private final String LABEL_250K = "250K";
    private final String LABEL_500K = "500K";
    private final String LABEL_800K = "800K";
    private final String LABEL_1M = "1M";
    private final int BITRATE_10K = 10000;
    private final int BITRATE_20K = 20000;
    private final int BITRATE_50K = 50000;
    private final int BITRATE_100K = 100000;
    private final int BITRATE_125K = 125000;
    private final int BITRATE_250K = 250000;
    private final int BITRATE_500K = 500000;
    private final int BITRATE_800K = 800000;
    private final int BITRATE_1M = 1000000;

    private boolean silentMode = false;
    private boolean termination = false;
	private boolean filtersEnabled = false;
    private boolean flowControlEnabled = false;
    private int baudRateSelected = BITRATE_250K;

    private Thread updateUIThread;

    private CanTest canTest;
    private TextView txtInterfaceClsTimeCan1;
    private TextView txtInterfaceOpenTimeCan1;
    private TextView txtCanTxSpeedCan1;
    private TextView txtCanBaudRateCan1;

    private TextView textViewFramesRx;
    private TextView textViewFramesTx;

    private CanFramesViewModel mCanFramesViewModel;

    // Socket dependent UI
    private Button btnTransmitCAN1;
    private ToggleButton swCycleTransmitJ1939Can1;
    private SeekBar seekBarJ1939SendCan1;

    //Interface dependent UI
    private ToggleButton toggleButtonTermCan1;
    private ToggleButton toggleButtonListenCan1;
    private Spinner spinnerBitrateCan1;
    private ToggleButton toggleButtonFilterSetCan1;
    private ToggleButton toggleButtonFlowControlCan1;

    private Button openCan1;
    private Button closeCan1;

    private ChangeBaudRateTask changeBaudRateTask;

    private int mDockState = -1;
    private boolean reopenCANOnTtyAttachEvent = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        //canTest = CanTest.getInstance();
        canTest = new CanTest(CanTest.CAN_PORT1);
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");


        Context context = getContext();
        if (context != null){
            LocalBroadcastManager.getInstance(context).registerReceiver(broadcastReceiver, DeviceStateReceiver.getLocalIntentFilter());
        }
        this.mDockState = MainActivity.getDockState();
        updateCradleIgnState();
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
        boolean open = canTest.isPortSocketOpen();
        btnTransmitCAN1.setEnabled(open);
        swCycleTransmitJ1939Can1.setEnabled(open);
        seekBarJ1939SendCan1.setEnabled(open);
    }

    private void setDockStateDependentUI(){
        boolean uiElementEnabled = true;
        if (mDockState == Intent.EXTRA_DOCK_STATE_UNDOCKED){
            uiElementEnabled = false;
        }
        toggleButtonTermCan1.setEnabled(uiElementEnabled);
        toggleButtonListenCan1.setEnabled(uiElementEnabled);
        spinnerBitrateCan1.setEnabled(uiElementEnabled);
        toggleButtonFilterSetCan1.setEnabled(uiElementEnabled);
        toggleButtonFlowControlCan1.setEnabled(uiElementEnabled);
        openCan1.setEnabled(uiElementEnabled);
        closeCan1.setEnabled(uiElementEnabled);
    }

    private void updateInterfaceStatusUI(String status) {
        final TextView txtInterfaceStatus = rootView.findViewById(R.id.textCan1InterfaceStatus);
        if(status != null) {
            txtInterfaceStatus.setText(status);
            txtInterfaceStatus.setBackgroundColor(Color.YELLOW);
        } else if(canTest.isCanInterfaceOpen()) {
            txtInterfaceStatus.setText(getString(R.string.open));
            txtInterfaceStatus.setBackgroundColor(Color.GREEN);
        } else { // closed
            txtInterfaceStatus.setText(getString(R.string.closed));
            txtInterfaceStatus.setBackgroundColor(Color.RED);
        }

        final TextView txtSocketStatus = rootView.findViewById(R.id.textCan1SocketStatus);
        if(status != null) {
            txtSocketStatus.setText(status);
            txtSocketStatus.setBackgroundColor(Color.YELLOW);
        } else if(canTest.isPortSocketOpen()) {
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

    private void openCan1Interface(){
        canTest.setRemoveCanInterfaceState(false);
        canTest.setBaudRate(baudRateSelected);
        canTest.setPortNumber(2);
        canTest.setSilentMode(silentMode);
        canTest.setTermination(termination);
        canTest.setRemoveCanInterfaceState(false);
        canTest.setFiltersEnabled(filtersEnabled);
        canTest.setFlowControlEnabled(flowControlEnabled);
        executeChangeBaudRate();
    }

    private void closeCan1Interface(){
        canTest.setRemoveCanInterfaceState(true);
        clearUIFrameCounts();
        executeChangeBaudRate();
    }

    private void
    executeChangeBaudRate() {
        if (changeBaudRateTask == null || changeBaudRateTask.getStatus() != AsyncTask.Status.RUNNING) {
            changeBaudRateTask = new ChangeBaudRateTask( silentMode , baudRateSelected, termination, canTest.getPortNumber(), filtersEnabled, flowControlEnabled);
            changeBaudRateTask.execute();
        }
    }

    private void updateRxFramesViewModel() {
        if (mCanFramesViewModel.can1FramesRxStr.getValue() == null){
            mCanFramesViewModel.can1FramesRxStr.setValue("");
        }
        mCanFramesViewModel.can1BytesRx.setValue(canTest.getPortCanbusRxByteCount());
        mCanFramesViewModel.can1FramesRx.setValue(canTest.getPortCanbusRxFrameCount());
        mCanFramesViewModel.can1FramesRxStr.setValue(mCanFramesViewModel.can1FramesRxStr.getValue() + canTest.canData);
        canTest.canData.setLength(0);
    }

    private void updateTxFramesViewModel(){
        if (mCanFramesViewModel.can1FramesTx.getValue() == null){
            mCanFramesViewModel.can1FramesTx.setValue(0);
        }
        mCanFramesViewModel.can1BytesTx.setValue(canTest.getPortCanbusTxByteCount());
        mCanFramesViewModel.can1FramesTx.setValue(canTest.getPortCanbusTxFrameCount());
    }

    private static int lastUIFramesUpdatedCountRx = 0;
    private static int lastUIFramesUpdatedCountTx = 0;
    private void updateCountUI() {

        if (canTest != null){
            if (canTest.getPortCanbusRxFrameCount() != lastUIFramesUpdatedCountRx){
                updateRxFramesViewModel();
                lastUIFramesUpdatedCountRx = canTest.getPortCanbusRxFrameCount();
            }

            if (canTest.getPortCanbusTxFrameCount() != lastUIFramesUpdatedCountTx){
                updateTxFramesViewModel();
                lastUIFramesUpdatedCountTx = canTest.getPortCanbusTxFrameCount();
            }
        }
    }

    private void updateBaudRateUI() {
        String baudRateDesc = getString(R.string._000k_desc);
        switch (canTest.getBaudRate()) {
            case BITRATE_10K:
                baudRateDesc = getString(R.string._10k_desc);
                break;
            case BITRATE_20K:
                baudRateDesc = getString(R.string._20k_desc);
                break;
            case BITRATE_50K:
                baudRateDesc = getString(R.string._50k_desc);
                break;
            case BITRATE_100K:
                baudRateDesc = getString(R.string._100k_desc);
                break;
            case BITRATE_125K:
                baudRateDesc = getString(R.string._125k_desc);
                break;
            case BITRATE_250K:
                baudRateDesc = getString(R.string._250k_desc);
                break;
            case BITRATE_500K:
                baudRateDesc = getString(R.string._500k_desc);
                break;
            case BITRATE_800K:
                baudRateDesc = getString(R.string._800k_desc);
                break;
            case BITRATE_1M:
                baudRateDesc = getString(R.string._1m_desc);
                break;
        }

        txtCanBaudRateCan1.setText(baudRateDesc);
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

        txtInterfaceOpenTimeCan1.setText(createdDate);
        txtInterfaceClsTimeCan1.setText(closedDate);
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
        mCanFramesViewModel.can1FramesTx.setValue(0);
        mCanFramesViewModel.can1BytesTx.setValue(0);
        mCanFramesViewModel.can1FramesRx.setValue(0);
        mCanFramesViewModel.can1BytesRx.setValue(0);
        mCanFramesViewModel.can1FramesRxStr.setValue("");
    }

    private void subscribeCan1Frames(){
        mCanFramesViewModel.can1FramesRx.observe(getActivity(), new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer can1FramesRx) {
                if (can1FramesRx != null){
                    String s1 = mCanFramesViewModel.can1FramesRx.getValue() + " Frames / " + mCanFramesViewModel.can1BytesRx.getValue() + " Bytes";
                    textViewFramesRx.setText(s1);
                    if (mCanFramesViewModel.can1FramesRx.getValue() == 0){
                        textViewFramesRx.setBackgroundColor(Color.WHITE);
                    }
                    else{
                        textViewFramesRx.setBackgroundColor(Color.GREEN);
                    }
                }
            }
        });

        mCanFramesViewModel.can1FramesTx.observe(getActivity(), new Observer<Integer>() {
            @Override
            public void onChanged(@Nullable Integer can1FramesTx) {
                if (can1FramesTx != null){
                    String s2 = "Tx: " + mCanFramesViewModel.can1FramesTx.getValue() + " Frames / " + mCanFramesViewModel.can1BytesTx.getValue() + " Bytes";
                    textViewFramesTx.setText(s2);
                    if (mCanFramesViewModel.can1FramesTx.getValue() == 0) {
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
        rootView = inflater.inflate(R.layout.fragment_can1_overview, container, false);

        mCanFramesViewModel = ViewModelProviders.of(getActivity()).get(CanFramesViewModel.class);

        textViewFramesRx = rootView.findViewById(R.id.textViewCan1FramesRx);
        textViewFramesTx = rootView.findViewById(R.id.textViewCan1FramesTx);
        spinnerBitrateCan1 = rootView.findViewById(R.id.spinnerCan1);
        toggleButtonListenCan1 = rootView.findViewById(R.id.toggleButtonCan1Listen);
        toggleButtonTermCan1 = rootView.findViewById(R.id.toggleButtonCan1Term);
        toggleButtonFilterSetCan1 = rootView.findViewById(R.id.toggleButtonCan1Filters);
        toggleButtonFlowControlCan1 = rootView.findViewById(R.id.toggleButtonCan1FlowControl);

        openCan1 = rootView.findViewById(R.id.buttonOpenCan1);
        closeCan1 = rootView.findViewById(R.id.buttonCloseCan1);
        txtInterfaceClsTimeCan1 = rootView.findViewById(R.id.textViewCan1ClosedTime);
        txtInterfaceOpenTimeCan1 = rootView.findViewById(R.id.textViewCan1CreatedTime);
        txtCanTxSpeedCan1 = rootView.findViewById(R.id.textViewCan1CurrTransmitInterval);
        txtCanBaudRateCan1 = rootView.findViewById(R.id.textViewCan1CurrBaudRate);

        btnTransmitCAN1 = rootView.findViewById(R.id.btnCan1SendJ1939);
        seekBarJ1939SendCan1 = rootView.findViewById(R.id.seekBarCan1SendSpeed);
        swCycleTransmitJ1939Can1 = rootView.findViewById(R.id.swCan1CycleTransmitJ1939);

        seekBarJ1939SendCan1.setProgress(canTest.getJ1939IntervalDelay());
        btnTransmitCAN1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                canTest.sendJ1939Port();
            }
        });

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(), R.array.bitrate_array, R.layout.spinner_item);
        adapter.setDropDownViewResource(R.layout.spinner_item);
        // Apply the adapter to the spinner
        spinnerBitrateCan1.setAdapter(adapter);
        spinnerBitrateCan1.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String text = spinnerBitrateCan1.getSelectedItem().toString();

                switch(text) {
                    case LABEL_10K:
                        baudRateSelected = BITRATE_10K;
                        break;
                    case LABEL_20K:
                        baudRateSelected = BITRATE_20K;
                        break;
                    case LABEL_50K:
                        baudRateSelected = BITRATE_50K;
                        break;
                    case LABEL_100K:
                        baudRateSelected = BITRATE_100K;
                        break;
                    case LABEL_125K:
                        baudRateSelected = BITRATE_125K;
                        break;
                    case LABEL_250K:
                        baudRateSelected = BITRATE_250K;
                        break;
                    case LABEL_500K:
                        baudRateSelected = BITRATE_500K;
                        break;
                    case LABEL_800K:
                        baudRateSelected = BITRATE_800K;
                        break;
                    case LABEL_1M:
                        baudRateSelected = BITRATE_1M;
                        break;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        // Default to 250K to start with.
        spinnerBitrateCan1.setSelection(5);

        toggleButtonTermCan1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                termination = isChecked;
            }
        });


        toggleButtonListenCan1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                silentMode = isChecked;
            }
        });

        toggleButtonFilterSetCan1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                filtersEnabled = isChecked;
            }
        });

        toggleButtonFlowControlCan1.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                flowControlEnabled = isChecked;
            }
        });

        openCan1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openCan1Interface();
            }
        });

        closeCan1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                closeCan1Interface();
            }
        });

        swCycleTransmitJ1939Can1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                canTest.setAutoSendJ1939Port(swCycleTransmitJ1939Can1.isChecked());
                canTest.sendJ1939Port();
            }
        });

        seekBarJ1939SendCan1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    canTest.setJ1939IntervalDelay(progress);
                    String progressStr = String.valueOf(progress) + "ms";
                    txtCanTxSpeedCan1.setText(progressStr);
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
        updateBaudRateUI();
        updateInterfaceTime();
        updateInterfaceStatusUI();
        setStateSocketDependentUI();
        setDockStateDependentUI();
        subscribeCan1Frames();
        Log.d(TAG, "onCreateView end");
        return rootView;
    }

    private class ChangeBaudRateTask extends AsyncTask<Void, String, Void> {

        final int baudRate;
        final boolean silent;
        final boolean termination;
        final int port;
		final boolean filtersEnabled;
        final boolean flowControlEnabled;
        int ret = -9;

        private ChangeBaudRateTask(boolean silent, int baudRate, boolean termination, int port, boolean filtersEnabled, boolean flowControlEnabled) {
            this.baudRate = baudRate;
            this.silent = silent;
            this.termination=termination;
            this.port=port;
            this.filtersEnabled = filtersEnabled;
            this.flowControlEnabled = flowControlEnabled;
        }

        @Override
        protected Void doInBackground(Void... params) {
            LastClosed = Calendar.getInstance().getTime();
            if(canTest.isCanInterfaceOpen() || canTest.isPortSocketOpen()) {
                publishProgress("Closing interface, please wait...");
                canTest.closeCanInterface();
                publishProgress("Closing socket, please wait...");
                canTest.closeCanSocket();
                return null;
            }

            publishProgress("Opening, please wait...");
            ret = canTest.CreateCanInterface(silent, baudRate, termination, port, filtersEnabled, flowControlEnabled);
            if (ret == 0) {
                LastCreated = Calendar.getInstance().getTime();
            }
            else{
                publishProgress("Closing interface, please wait...");
                canTest.closeCanInterface();
                publishProgress("Closing socket, please wait...");
                canTest.closeCanSocket();
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
            updateBaudRateUI();
            updateInterfaceTime();
            startUpdateUIThread();
            updateInterfaceStatusUI();
            setStateSocketDependentUI();
            setDockStateDependentUI();

            Toast toast;
            TextView v;
            switch(ret) {
                case CanbusException.GENERAL_ERROR:
                    Toast.makeText(getContext(), "General error.", Toast.LENGTH_LONG).show();
                    break;
                case CanbusException.INVALID_PARAMETERS:
                    toast = Toast.makeText(getContext(), "Invalid parameters passed to CanbusInterface.", Toast.LENGTH_LONG);
                    v = toast.getView().findViewById(android.R.id.message);
                    if( v != null) v.setGravity(Gravity.CENTER);
                    toast.show();
                    break;
                case CanbusException.PORT_DOESNT_EXIST:
                    toast = Toast.makeText(getContext(), "Port doesn't exist.\nRedock or restart device to fix.", Toast.LENGTH_LONG);
                    v = toast.getView().findViewById(android.R.id.message);
                    if( v != null) v.setGravity(Gravity.CENTER);
                    toast.show();
                    break;
                default:
                    break;
            }
        }
    }

    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            goAsync();

            String action = intent.getAction();

            if (action != null) {
                switch (action) {
                    case "com.micronet.smarttabsmarthubsampleapp.dockevent":
                        mDockState = intent.getIntExtra(android.content.Intent.EXTRA_DOCK_STATE, -1);
                        updateCradleIgnState();
                        Log.d(TAG, "Dock event received: " + mDockState);
                        break;
                    case "com.micronet.smarttabsmarthubsampleapp.portsattached":
                        if (reopenCANOnTtyAttachEvent){
                            Log.d(TAG, "Reopening CAN1 port since the tty port attach event was received");
                            Toast.makeText(getContext().getApplicationContext(), "Reopening CAN1 port since the tty port attach event was received",
                                    Toast.LENGTH_SHORT).show();
                            openCan1Interface();
                            reopenCANOnTtyAttachEvent = false;
                        }
                        Log.d(TAG, "Ports attached event received");
                        break;
                    case "com.micronet.smarttabsmarthubsampleapp.portsdetached":
                        if (canTest.isCanInterfaceOpen()){
                            Log.d(TAG, "closing CAN1 port since the tty port detach event was received");
                            Toast.makeText(getContext().getApplicationContext(), "closing CAN1 port since the tty port detach event was received",
                                    Toast.LENGTH_SHORT).show();
                            closeCan1Interface();
                            reopenCANOnTtyAttachEvent = true;
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
