# MultiBLECommunication
Multiple BLE connections in a fast and simple way.

## Download
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
    implementation 'com.github.nisargbanker-prompt:MultiBLECommunication:1.0.2'
}
```

## Usage
Initialize the library in your Application class.
```java
PromptBLE.init(applicationContext)
```

You can start the BLE service when you want to use a Bluetooth connection.
```java
PromptUtils.startBLEService(this);
```
Scan devices
```java
PromptUtils.scanBLEDevices()
```
Connect and hold the BLE key that you provided in the function below for more functions.
```java
PromptUtils.setOnClickOfBluetoothDevice(key = "BLE1", bleObject)
```

Observe connection states using the below method.
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

Observe BLE data from the below method.
```java
PromptUtils.mutableDataFromBLE.observeForever {
            DLog.e("BLE Data - - - ", it.second)
        }
```

Write data on BLE using below method.
```java
PromptUtils.writeData(key = "BLE1", "Write data string".toByteArray())
```

Disconnect while destroy activity
```java
PromptUtils.disconnectAll()
```
