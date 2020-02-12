/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.vehiclebussample.fragments;

import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModelProviders;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.micronet.vehiclebussample.J1708FramesViewModel;
import com.micronet.vehiclebussample.R;

/**
 * Created by Scott Krstyen on 12/17/2018.
 */

public class J1708FramesFragment extends Fragment {
    private TextView lvJ1708FramesPort;
    private J1708FramesViewModel j1708FramesViewModel;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onStart() {
        super.onStart();
    }

    private void subscribeJ1708Frames(){
        j1708FramesViewModel.j1708FramesRxStr.observe(getActivity(), new Observer<String>() {
            @Override
            public void onChanged(@Nullable String j1708FramesRx) {
                if (j1708FramesRx != null && j1708FramesViewModel.j1708FramesRxStr.getValue().length() != 0){
                    lvJ1708FramesPort.setText(j1708FramesViewModel.j1708FramesRxStr.getValue());

                    //we want to limit the maximum number of frames being stored in lvJ1708FramesPort
                    //the side effect of this is that the frames screen could go blank a little bit
                    String data = j1708FramesViewModel.j1708FramesRxStr.getValue();
                    if (data.length() > 10000){
                        String substring = data.substring(9000);
                        lvJ1708FramesPort.setText(substring);
                        j1708FramesViewModel.j1708FramesRxStr.setValue(substring);
                    }
                }
            }
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_j1708_frames, container, false);

        j1708FramesViewModel = ViewModelProviders.of(getActivity()).get(J1708FramesViewModel.class);

        lvJ1708FramesPort = view.findViewById(R.id.lvJ1708FramesPort);
        lvJ1708FramesPort.setMovementMethod(new ScrollingMovementMethod());

        subscribeJ1708Frames();
        return view;
    }
}
