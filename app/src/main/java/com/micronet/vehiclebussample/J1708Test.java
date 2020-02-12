/*
 * This file is subject to the terms and conditions defined in
 * file 'LICENSE.txt', which is part of this source code package.
 */

package com.micronet.vehiclebussample;

import static java.lang.Thread.sleep;

import android.os.SystemClock;
import android.util.Log;
import com.micronet.canbus.CanbusException;
import com.micronet.canbus.CanbusFlowControl;
import com.micronet.canbus.CanbusFramePort1;
import com.micronet.canbus.CanbusFramePort2;
import com.micronet.canbus.CanbusFrameType;
import com.micronet.canbus.CanbusHardwareFilter;
import com.micronet.canbus.CanbusInterface;
import com.micronet.canbus.CanbusSocket;
import com.micronet.canbus.J1708Frame;
import com.micronet.canbus.J1708Interface;
import com.micronet.canbus.J1708Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;

public class J1708Test {

    private final static String TAG = "J1708Test";

    private J1708Interface j1708Interface;
    private J1708Socket j1708Socket;

    public final StringBuilder j1708Data = new StringBuilder(1000);

    private int j1708IntervalDelay = 500; // ms

    private Thread j1708PortReaderThread = null;
    private Thread j1708PortWriterThread = null;

    private J1708PortReader j1708PortReader = null;
    private J1708PortWriter j1708PortWriter = null;

    private volatile boolean blockOnReadPort = true;
    private final int READ_TIMEOUT = 100; // readPort1 timeout (in milliseconds)

    private volatile boolean autoSendJ1708Port;
    private boolean isJ1708InterfaceOpen = false;

    private int j1708MessageIdPort;
    private byte[] j1708MessageDataPort;

    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    private boolean removeJ1708;

    public J1708Test() {
        //Mandatory constructor
    }

//    // Lazy Initialization (If required then only)
//    public static CanTest getInstance() {
//        if (instance == null) {
//            // Thread Safe. Might be costly operation in some case
//            synchronized (CanTest.class) {
//                if (instance == null) {
//                    instance = new CanTest();
//                }
//            }
//        }
//        return instance;
//    }

    public boolean isPortSocketOpen() {
        // there's actually no api call to check status of j1708 socket but
        // this app will open the socket as soon as object is initialized.
        return j1708Socket != null;
    }

    public boolean isJ1708InterfaceOpen() {
        return isJ1708InterfaceOpen;
    }

    public boolean getRemoveJ1708InterfaceState() {
        return removeJ1708;
    }

    public void setRemoveJ1708InterfaceState(boolean removeCan) {
        this.removeJ1708 = removeJ1708;
    }

    public int createJ1708Interface() {

        if (j1708Interface == null) {
            j1708Interface = new J1708Interface();
            j1708Interface.createJ1708();
        }

        if(j1708Socket == null){
            j1708Socket = j1708Interface.createSocketJ1708();
            j1708Socket.openJ1708();
        }
        isJ1708InterfaceOpen = true;
        startJ1708Threads();
        return 0;
    }

    private void startJ1708Threads() {
        if (j1708PortReader == null) {
            j1708PortReader = new J1708PortReader();
        }

        j1708PortReader.clearValues();

        if (j1708PortReaderThread == null || j1708PortReaderThread.getState() != Thread.State.NEW) {
            j1708PortReaderThread = new Thread(j1708PortReader);
        }

        j1708PortReaderThread.setPriority(Thread.NORM_PRIORITY + 3);
        j1708PortReaderThread.start();
    }

    public void closeJ1708Interface() {
        // TODO add code to return from remove J1708

        if (j1708Interface != null) {
            j1708Interface.removeJ1708();

            j1708Interface = null;
        }
        isJ1708InterfaceOpen = false;
    }

    public void closeJ1708Socket() {
        if (isPortSocketOpen()) {
            if (j1708PortReaderThread != null && j1708PortReaderThread.isAlive()) {
                j1708PortReaderThread.interrupt();
                try {
                    j1708PortReader.stopRunnable();
                    j1708PortReaderThread.join(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (j1708PortWriterThread != null && j1708PortWriterThread.isAlive()) {
                j1708PortWriterThread.interrupt();
                try {
                    j1708PortWriterThread.join(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (j1708PortWriter != null){
                j1708PortWriter.clearCounts();
            }

            j1708Socket.close1708Port();

            j1708Socket = null;
        }

    }

    /// J1708 Reader
    public int getPortJ1708RxFrameCount() {
        if (j1708PortReader == null)
            return 0;
        return j1708PortReader.getJ1708FrameCount();
    }

    public int getPortJ1708RxByteCount() {
        if (j1708PortReader == null)
            return 0;
        return j1708PortReader.getJ1708ByteCount();
    }

    public int getPortJ1708TxFrameCount(){
        if (j1708PortWriter == null){
            return 0;
        }
        return j1708PortWriter.getFrameCount();
    }

    public int getPortJ1708TxByteCount(){
        if (j1708PortWriter == null){
            return 0;
        }
        return j1708PortWriter.getByteCount();
    }

    public boolean isAutoSendJ1708Port() {
        return autoSendJ1708Port;
    }

    public void setAutoSendJ1708Port(boolean autoSendJ1708Port) {
        this.autoSendJ1708Port = autoSendJ1708Port;
    }

    private class J1708PortReader implements Runnable {
        private volatile int j1708FrameCount = 0;
        private volatile int j1708ByteCount = 0;
        private volatile boolean keepRunning = true;

        int getJ1708FrameCount() {
            return j1708FrameCount;
        }

        int getJ1708ByteCount() {
            return j1708ByteCount;
        }

        void clearValues() {
            j1708ByteCount = 0;
            j1708FrameCount = 0;
            keepRunning = true;
        }

        void stopRunnable(){
            keepRunning = false;
        }

        private void getJ1708Frames(){
            J1708Frame j1708Frame;
            try {
                if (j1708Socket == null){
                    return;
                }

                if (blockOnReadPort) {
                    j1708Frame = j1708Socket.readJ1708Port();
                } else {
                    j1708Frame = j1708Socket.readJ1708Port(READ_TIMEOUT);
                }
                if (j1708Frame != null)
                {
                    long time = SystemClock.elapsedRealtime();
                    // done to prevent adding too much text to UI at once
                    //if (canData.length() < 500) {
                        // avoiding string.format for performance
                    j1708Data.append(time);
                    j1708Data.append(", ");
                    j1708Data.append(Integer.toHexString(j1708Frame.getId()));
                    j1708Data.append(", [");
                    j1708Data.append(bytesToHex(j1708Frame.getData()));
                    j1708Data.append("], ");
                    j1708Data.append(j1708Frame.getData().length);
                    j1708Data.append("\n");
                    //}

                    // TODO non-atomic operation on volatile field
                    j1708FrameCount++;
                    j1708ByteCount += j1708Frame.getData().length;
                }
                //else {
                // Log.d(TAG, "Read timeout");
                //}
            }catch (NullPointerException ex) {
                return;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }



        @Override
        public void run() {
            Log.d(TAG, "J1708PortReader started");
            while (keepRunning) {
                getJ1708Frames();
            }
            Log.d(TAG, "J1708PortReader exited");
        }
    }

    public int getJ1708IntervalDelay() {
        return j1708IntervalDelay;
    }

    public void setJ1708IntervalDelay(int j1708IntervalDelay) {
        this.j1708IntervalDelay = j1708IntervalDelay;
    }

    // Convert a byte array to a hex friendly string
    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 3];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 3] = HEX_ARRAY[v >>> 4];
            hexChars[j * 3 + 1] = HEX_ARRAY[v & 0x0F];
            hexChars[j * 3 + 2] = ' ';
        }
        return new String(hexChars);
    }

    public void sendJ1708Port() {
        if (j1708PortWriter == null){
            j1708PortWriter = new J1708PortWriter();
        }
        if (j1708PortWriterThread == null || !j1708PortWriterThread.isAlive()) {
            j1708PortWriterThread = new Thread(j1708PortWriter);
            j1708PortWriterThread.start();
        }
    }

    public void sendJ1708Port(String messageId, String messageData){
        j1708MessageDataPort = messageData.getBytes();
        j1708MessageIdPort = Integer.parseInt(messageId);

        if (j1708PortWriterThread == null || !j1708PortWriterThread.isAlive()) {
            j1708PortWriterThread = new Thread(sendJ1708PortRunnable2);
            j1708PortWriterThread.start();
        }
    }

    private class J1708PortWriter implements Runnable{
        private int sentFrameCount = 0;
        private int sentByteCount = 0;

        void clearCounts(){
            this.sentByteCount = 0;
            this.sentFrameCount = 0;
        }

        private int getFrameCount(){
            return sentFrameCount;
        }

        private int getByteCount(){
            return sentByteCount;
        }

        @Override
        public void run() {
            int MessageId;
            do {

                MessageId = 0x31;
                int data = 0;
                byte[] a = new byte[5];
                a[0] = 0x6A;
                a[1] = 0x31;
                a[2] = 0x37;
                a[3] = 0x30;
                a[4] = 0x38;

//                ByteBuffer dBuf = ByteBuffer.allocate(8);
//                dBuf.order(ByteOrder.LITTLE_ENDIAN);
//                dBuf.putInt(data);
//                byte[] a = dBuf.array();
//                a[0] = 0x12;
//                a[1] = 0x34;
//                a[2] = 0x45;
//                a[3] = 0x67;
//                a[4] = 0x1F;
//                a[5] = 0x2F;
//                a[6] = 0x3F;
//                a[7] = 0x4F;
//                MessageData=a;

                Log.d(TAG, "In J1708 port writer.");

                if(j1708Socket != null) {
                    J1708Frame j1708Frame = new J1708Frame(MessageId, a);
                    j1708Socket.writeJ1708Port(j1708Frame);
                    sentByteCount += j1708Frame.getData().length;
                    sentFrameCount++;
                    Log.d(TAG, "Sent J1708 message.");
                }
                try {
                    sleep(j1708IntervalDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (autoSendJ1708Port);
        }
    }

    private final Runnable sendJ1708PortRunnable2 = new Runnable() {
        @Override
        public void run() {
            do {
                if(j1708Socket != null) {

                    j1708Socket.writeJ1708Port(new J1708Frame(j1708MessageIdPort, j1708MessageDataPort));
                }
                try {
                    sleep(j1708IntervalDelay);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } while (autoSendJ1708Port);
        }
    };

    /// End J1708 methods

}
