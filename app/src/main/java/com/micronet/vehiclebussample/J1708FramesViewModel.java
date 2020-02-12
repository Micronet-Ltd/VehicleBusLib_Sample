/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.vehiclebussample;

import android.arch.lifecycle.MutableLiveData;
import android.arch.lifecycle.ViewModel;

public class J1708FramesViewModel extends ViewModel {
    public MutableLiveData<Integer> j1708FramesRx = new MutableLiveData<>();
    public MutableLiveData<Integer> j1708BytesRx = new MutableLiveData<>();

    public MutableLiveData<String> j1708FramesRxStr = new MutableLiveData<>();

    public MutableLiveData<Integer> j1708FramesTx = new MutableLiveData<>();
    public MutableLiveData<Integer> j1708BytesTx = new MutableLiveData<>();
}