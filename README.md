# Vehicle Bus Library Sample

This application uses the latest version of the Vehicle Bus Library to communicate over Canbus. 

**Note**: To use the Vehicle Bus Library or build this sample app properly, your application must be signed by the platform key.

Features:
- Configure CAN1/CAN2 to a certain bitrate.
- Use preconfigured filters.
- Use preconfigured flow controls.
- Use listen only mode.
- Use termination.
- Transmit Canbus frames.
- Counts the number of received and sent frames and displays them.


### How to Use
When the app starts, you will be on the Can1 overview fragment.

![alt text](https://raw.githubusercontent.com/Micronet-Ltd/VehicleBusLib_Sample/master/images/start.png "")

You can change the selected bitrate by clicking on the dropdown.

![alt text](https://raw.githubusercontent.com/Micronet-Ltd/VehicleBusLib_Sample/master/images/spinner.png "")

Then you can create the canbus interface by clicking the "Create Can Interface" button.

![alt text](https://raw.githubusercontent.com/Micronet-Ltd/VehicleBusLib_Sample/master/images/create.png "")

When frames are sent successfully, you will see the the "Frames Rx" count go up.

![alt text](https://raw.githubusercontent.com/Micronet-Ltd/VehicleBusLib_Sample/master/images/receiving.png "")

You can use the Can Frames tab to view the received canbus frames.

![alt text](https://raw.githubusercontent.com/Micronet-Ltd/VehicleBusLib_Sample/master/images/canframes.png "")

The Info tab give you some information about the device and what version of the Vehicle Bus Library is being used.

![alt text](https://raw.githubusercontent.com/Micronet-Ltd/VehicleBusLib_Sample/master/images/info.png "")

### Known Issues on the Tab8
Sometimes on the Tab8 the ports to communicate over Canbus will not enumerate on the OS side. When the app tries to create an interface and the port doesn't exist, then a toast will be created that says to redock or restart the device to resolve the issue.


### Adding the Vehicle Bus Library to the Android App 
To import the library, add the Vehiclebus_api.jar file to the ‘app\libs\ folder’ and the libvehiclebus.so file to the ‘app\src\main\jniLibs\armeabi-v7a’ folder. You will need to create the folder jniLibs/armeabi-v7a.  
Note: These instructions are for the 32 bit version of the library 

For Android Studio to detect the library files, please the following to the application gradle file:
* abiFilters "armeabi", "armeabi-v7a", "x86", "mips"
* implementation files('libs/vehicleBusApi.jar')
* implementation fileTree(dir: 'libs',include: ['*.jar', '*.aar', "*.so"])

```java
android { 
    compileSdkVersion 28
    buildToolsVersion '28.0.3'
    defaultConfig {
        applicationId "com.micronet.vehiclebussample"
        minSdkVersion 28
        targetSdkVersion 28
        versionCode 2
        versionName "1.0.1"
        setProperty("archivesBaseName", "VehicleBusSample-v" + versionName)
        testInstrumentationRunner "android.support.test.runner.AndroidJUnitRunner"
        ndk{
            abiFilters "armeabi", "armeabi-v7a", "x86", "mips"
        }
    }
} 
```
``` java
dependencies { 
    implementation fileTree(dir: 'libs',include: ['*.jar', '*.aar', "*.so"]) 
    implementation 'com.android.support:appcompat-v7:28.0.0'
    implementation 'com.android.support.constraint:constraint-layout:1.1.3'
    implementation 'com.android.support:support-v4:28.0.0'
} 
```

### Setting up a CAN Interface
There are multiple methods available to create an interface. We will discuss the creation of an interface with filters and flow control. To create a new CAN interface, we need to first create a new CanbusInterface() object. We then need to pass in the arguments to the create method. In this example we are creating an interface to CAN1 (portNumber = 2). 
``` java
private CanbusInterface canBusInterface1; 
private CanbusHardwareFilter[] canBusFilter; 
private CanbusFlowControl[] canBusFlowControls; 
private CanbusSocket canBusSocket1; 
int portNumber = 2; 

if (canBusInterface1 == null) { 
    canBusInterface1 = new CanbusInterface(); 
    canBusFilter = setFilters(); 
    canBusFlowControls = setFlowControlMessages(); 
    try { 
        canBusInterface1.create(silentMode, baudRate, termination, canBusFilter, portNumber, canBusFlowControls); 
    } catch (CanbusException e) { 
        Log.e(TAG, e.getMessage() + ", errorCode = " + e.getErrorCode()); 
        e.printStackTrace(); 
        return -1; 
    } 
} 
 
if (canBusSocket1 == null) { 
    canBusSocket1 = canBusInterface1.createSocketCAN1(); 
    canBusSocket1.openCan1(); 
} 
if (discardInBuffer) { 
    canBusSocket1.discardInBuffer(); 
} 
isCan1InterfaceOpen = true; 
startPort1Threads(); 
return 0; 
```
It is important to check for any CanbusExceptions that occur when creating the interface. If the interface is created successfully, we can create a socket to CAN1, discard the old buffer and create a thread for reading the CAN bus frames. 

 

Below we describe the filter and flow control arguments of the create interface:  
#### Filters 
Filters are used to only allow certain messages to come through on the CAN bus. To see an example of setting up filters, take a look at setFilters() in the CanTest.java class. Filters are defined as an array of CanbusHardwareFilter[]. Each filter contains a filter type, an ID to search for and mask. The smart cradle does not allow any messages through if no filter is provided. To allow all messages through, the user can set a standard and extended ID and mask of 0x0, as shown below: 

``` java
ArrayList<CanbusFilter> filterList = new ArrayList<>();
CanbusFilter[] filters;

if (enableFilters) {
    filterList.add(new CanbusFilter(0x18FEE000, 0x1FFFFFFF, CanbusFilter.EXTENDED));
    filterList.add(new CanbusFilter(0x1CECFF00, 0x1FFF00FF, CanbusFilter.EXTENDED));
    filterList.add(new CanbusFilter(0x1CEBFF00, 0x1FFF00FF, CanbusFilter.EXTENDED));
    filterList.add(new CanbusFilter(0x18FEE500, 0x1FFFFFFF, CanbusFilter.EXTENDED));
    filterList.add(new CanbusFilter(0x18FEF100, 0x1FFFFFFF, CanbusFilter.EXTENDED));
    filterList.add(new CanbusFilter(J1939_ENGINE_CONTROLLER2 << 8, 0x00FFFF00, CanbusFilter.EXTENDED));
    filterList.add(new CanbusFilter(J1939_ENGINE_CONTROLLER1 << 8, 0x00FFFF00, CanbusFilter.EXTENDED));
    filterList.add(new CanbusFilter(J1939_PGN_DASH_DISP << 8, 0x00FFFF00, CanbusFilter.EXTENDED));
    filterList.add(new CanbusFilter(J1939_PGN_GEAR << 8, 0x00FFFF00, CanbusFilter.EXTENDED));
    filterList.add(new CanbusFilter(J1939_ENGINE_TEMPERATURE_1 << 8, 0x00FFFF00, CanbusFilter.EXTENDED));
    filterList.add(new CanbusFilter(J1939_FUEL_ECONOMY << 8, 0x00FFFF00, CanbusFilter.EXTENDED));
}
else{
    // Setting up two filters: one standard and one extended to allow all messages.
    filterList.add(new CanbusFilter(CanbusFilter.DEFAULT_FILTER, CanbusFilter.DEFAULT_MASK, CanbusFilter.STANDARD));
    filterList.add(new CanbusFilter(CanbusFilter.DEFAULT_FILTER, CanbusFilter.DEFAULT_MASK, CanbusFilter.EXTENDED));
}

// The filters array to pass to the createInterface method.
filters = filterList.toArray(new CanbusFilter[0]);
```
 
NOTE: Users are required to pass a filters list to the create interface. The filter list cannot be null 

#### Flow Control 
Flow control is used to send a message response automatically on the CAN bus by the MCU firmware. This is useful when user needs to send responses to messages very quickly on the CAN bus. Flow control message requests have the first byte as 0x10. To see an example, take a look at the setFlowControlMessages() in the CanTest.java class. 
``` java
byte[] responseData=new byte[]{0x10,0x34,0x56,0x78,0x1f,0x2f,0x3f,0x4f}; 
CanbusFlowControl[] flowControlMessages = new CanbusFlowControl[1]; 

flowControlMessages[0] = new CanbusFlowControl(0x18FEE000,0x18FEE018,CanbusFlowControl.EXTENDED,8,responseData); 
```

In the example above, if a message is received with ID 0x18FEE000 and the data byte 0 is 0x10, the MCU will respond with ID 0x18FEE000 and data bytes {0x10,0x34,0x56,0x78,0x1f,0x2f,0x3f,0x4f} 

### Sending and Receiving frames to the CAN ports 
#### Sending Frames 
We send frames by creating a J1939Port1Writter Runnable. In the runnable we create a new CanbusFramePort1 object with the messageId, messageData and messageType. We then write the frame to the port using canBusSocket1.write1939Port1(canFrame). In the example application we are writing the same frame every time. 
``` java
CanbusFramePort1 canFrame = new CanbusFramePort1(MessageId, MessageData, MessageType); 
canBusSocket1.write1939Port1(canFrame); 
```

#### Receiving Frames 
We receive frames by creating a J1939Port1Reader Runnable. In the runnable we have two options, we can either read the port continuously or we can provide a read timeout.
``` java
private volatile boolean blockOnReadPort1 = false; 
CanbusFramePort1 canBusFrame1; 

if (blockOnReadPort1) { 
    canBusFrame1 = canBusSocket1.readPort1(); 
} else { 
    canBusFrame1 = canBusSocket1.readPort1(READ_TIMEOUT); 
} 
```
Once we receive a frame, we parse the frame and write it to the CANFrame fragment text UI. Example code can be seen in the CanTest.java class.

### Handling Vehicle Bus exceptions 
When opening an interface, it is important to handle the Vehicle bus exceptions as shown below. If the device is not docked, the Canbus library will not be able to communicate with the MCU and will return a vehicle bus exception with the corresponding error code. The error code will be -1 for a general error, -2 for invalid arguments passed, and -3 if the port doesn't exist. 
``` java
if (canBusInterface1 == null) { 
    canBusInterface1 = new CanbusInterface(); 
    canBusFilter = setFilters(); 
    canBusFlowControls = setFlowControlMessages(); 
    try { 
        canBusInterface1.create(silentMode, baudRate, termination, canBusFilter, portNumber, canBusFlowControls); 
    } catch (CanbusException e) { 
        Log.e(TAG, e.getMessage() + ", errorCode = " + e.getErrorCode()); 
        e.printStackTrace(); 
        int errorCode = e.getErrorCode();
        switch(errorCode) {
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
        return errorCode; 
    } 
} 
```

### Handling USB port changes when the USB ports are detached  
When the MT5 is undocked from the smart cradle, the application will need to close out the CAN interface and reopen it when it is docked back into the cradle 

Each Can Overview fragments registers for a local broadcast (described in section ‘Device Docking and USB events’) shown below: 
``` java
IntentFilter filters = new IntentFilter(); 
filters.addAction("com.micronet.smarttabsmarthubsampleapp.dockevent"); 
filters.addAction("com.micronet.smarttabsmarthubsampleapp.portsattached"); 
filters.addAction("com.micronet.smarttabsmarthubsampleapp.portsdetached"); 
```

If the CAN interface is Open and the fragment gets a portdetached event, the state of the CAN interface is remembered and when the portattached event is received, the fragment reopens the CAN interface with the stored configuration.  
``` java
private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { 
    @Override 
    public void onReceive(Context context, Intent intent) { 
        goAsync(); 
 
        String action = intent.getAction(); 
 
        if (action != null) { 
            switch (action) { 
                case "com.micronet.smarttabsmarthubsampleapp.portsattached": 
                    if (reopenCANOnTtyAttachEvent){ 
                        openCan1Interface(); 
                        reopenCANOnTtyAttachEvent = false; 
                    } 
                    Log.d(TAG, "Ports attached event received"); 
                    break; 
                case "com.micronet.smarttabsmarthubsampleapp.portsdetached": 
                    if (canTest.isCan1InterfaceOpen()){ 
                        closeCan1Interface(); 
                        reopenCANOnTtyAttachEvent = true; 
                    } 
                    Log.d(TAG, "Ports detached event received"); 
                    break; 
            } 
        } 
    } 
}; 
```
Note: When the MT5 is undocked the Cradle MCU automatically closes the CAN ports out to avoid anything being sent on the CAN bus when the cradle is in a standby state. 


## Device Docking and USB events
### Device State Receiver 
The app uses the Device State Receiver to receive dock events, USB attachment, and USB detachment broadcasts through an intent filter. The three broadcasts it receives are: 
* Intent.ACTION_DOCK_EVENT 
* UsbManager.ACTION_USB_DEVICE_ATTACHED 
* UsbManager.ACTION_USB_DEVICE_DETACHED 

More on dock events can be found here:  
https://developer.android.com/training/monitoring-device-state/docking-monitoring. 

Once it receives a broadcast it uses the LocalBroadcastManager to send a local broadcast to the app with the action that has occurred. Using the LocalBroadcastManager each fragment can register to receive the local broadcasts and then handle dock events and USB events as needed for that specific fragment. LocalBroadcasts are more efficient because they do not require inter-process communication since they are only sent to your app. 

More on Local Broadcasts:  

https://developer.android.com/reference/android/support/v4/content/LocalBroadcastManager. 

USB attachment and detachment events are especially important when dealing with the SmartTab and Smart Cradle because when the device is undocked it cannot communicate with the MCU.  

The Micronet Hardware Library should not be used until at least 2 seconds after the UsbManager.ACTION_USB_DEVICE_ATTACHED broadcast has been received. If it is used too quickly then it can fail to receive information back from the device because the underlying driver needs to get initialized and can take up 2 seconds to initialize after the USB ports are available.

The TTY ports can be used immediately after the UsbManager.ACTION_USB_DEVICE_ATTACHED broadcast is received. In this app that broadcast is received and then the CAN bus interface can be opened.  

### Getting Ignition through Dock Events 
When an Intent.ACTION_DOCK_EVENT is received it contains an integer extra stored with Intent.EXTRA_DOCK_STATE. There are five possible dock event states: 

* Intent.EXTRA_DOCK_STATE_UNDOCKED 
* Intent.EXTRA_DOCK_STATE_DESK 
* Intent.EXTRA_DOCK_STATE_LE_DESK 
* Intent.EXTRA_DOCK_STATE_HE_DESK 
* Intent.EXTRA_DOCK_STATE_CAR 

If the dock state is EXTRA_DOCK_STATE_UNDOCKED, then the device is undocked and cannot communicate with the MCU.  

If the dock state is EXTRA_DOCK_STATE_DESK, EXTRA_DOCK_STATE_LE_DESK, or EXTRA_DOCK_STATE_HE_DESK, then the device is docked but ignition is low. 

If the dock state is EXTRA_DOCK_STATE_CAR, then the device is docked and ignition is high.  

From the dock state you can tell what the current ignition state is. 

### Handling configuration changes during dock events 
When the device is docked and undocked, its configuration state changes. This causes the application to restart with the new configuration changes.  

As detailed here: https://developer.android.com/guide/topics/resources/runtime-changes, there are two ways that you can handle the configuration change. One is to retain an object during the configuration change and the other is to handle the configuration change yourself.  
