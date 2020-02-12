# Vehicle Bus Library Sample

This application uses the latest version of the Vehicle Bus Library to communicate over Canbus on the Tab8. 

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
