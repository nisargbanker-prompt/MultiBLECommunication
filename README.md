# MultiBLECommunication
This library allows you to manage multiple Bluetooth Low Energy (BLE) connections in a fast and simple way.

## Download
Gradle Dependency:
```gradle
dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url 'https://jitpack.io' }
		}
	}
```
```gradle
dependencies {
    implementation 'com.github.nisargbanker-prompt:MultiBLECommunication:1.0.6'
}
```

## Usage
### Initialization:
Initialize the library in your Application class.
* In your application class, initialize the library:
```java
PromptBLE.init(applicationContext)
```

### BLE Service:
* Start the BLE service when you need to use a Bluetooth connection:
```java
PromptUtils.startBLEService(this);
```

### Scanning for Devices:
* Use the `scanBLEDevices` method to scan for nearby BLE devices:
```java
PromptUtils.scanBLEDevices()
```

### Observing Devices:
* Subscribe to the `asyncDevices` observable to observe discovered devices.
```java
PromptUtils.asyncDevices.observeForever {
            //device = it
        }
```

### Connecting and Managing BLE Keys:
* Connect to a specific device by providing its key and a callback object.
```java
PromptUtils.setOnClickOfBluetoothDevice(key = "BLE1", bleObject)
```
* The `bleObject` will be used for further interaction with the connected device.

### Observing Connection State:
* Use the `selectedGattFlow` flow to observe changes in the connection state:
```java
lifecycleScope.launch {
            PromptUtils.selectedGattFlow.collectLatest {
                when (it.first) {
                    PromptUtils.ACTION_GATT_CONNECTED -> {
                        DLog.e("BLE - - - ", "ACTION_GATT_CONNECTED")
                    }

                    PromptUtils.ACTION_GATT_DISCONNECTED -> {
                        DLog.e("BLE - - - ", "ACTION_GATT_DISCONNECTED")
                    }
                }
            }
        }
```

### Observing BLE Data:
* Subscribe to the `mutableDataFromBLE` observable to receive data from the connected device:
```java
PromptUtils.mutableDataFromBLE.observeForever {
            DLog.e("BLE Data - - - ", it.second)
        }
```

### Writing Data to BLE:
* Use the `writeData` method to send data to the connected device:
```java
PromptUtils.writeData(key = "BLE1", "Write data string".toByteArray())
```
* Replace `"Write data string"` with your desired data.

### Disconnecting:
* Call `disconnectAll` when your activity is destroyed to disconnect from all connected devices.
```java
PromptUtils.disconnectAll()
```
