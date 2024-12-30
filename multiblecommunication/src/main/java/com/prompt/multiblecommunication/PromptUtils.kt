package com.prompt.multiblecommunication

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import com.prompt.multiblecommunication.BluetoothLeService.Companion.bluetoothAdapter
import kotlinx.coroutines.flow.MutableStateFlow

object PromptUtils {

    const val ACTION_GATT_CONNECTED =
        "com.example.bluetooth.le.ACTION_GATT_CONNECTED"
    const val ACTION_GATT_DISCONNECTED =
        "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED"
    const val ACTION_GATT_SERVICES_DISCOVERED =
        "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED"
    const val ACTION_DATA_AVAILABLE =
        "com.example.bluetooth.le.ACTION_DATA_AVAILABLE"
    const val ACTION_MTU_EXCHANGE =
        "android.bluetooth.device.action.mtuExchange"
    const val START_SCAN = "start_scan"
    const val STOP_SCAN = "stop_scan"

    const val TAG: String = "PromptUtils"

    var mutableDataFromBLE = MutableLiveData<Pair<String, String>>()

    var mutableDisconnectedBLE = MutableLiveData<String>()

    var selectedGattFlow = MutableStateFlow(Pair("", BluetoothLeService.tempBleGatt))

    var selectedGattList = ArrayList<Pair<String, BluetoothGatt>>()

    var gattServicesList = ArrayList<Pair<String, String>>()

    var gattRxCharacteristicList = ArrayList<Pair<String, String>>()

    var gattTxCharacteristicList = ArrayList<Pair<String, String>>()

    var bluetoothService: BluetoothLeService? = null

    var deviceSet: MutableSet<BluetoothDevice> = mutableSetOf()

    var allDeviceList = ArrayList<BluetoothDevice>()

    val scanResults = mutableListOf<ScanResult>()

    var asyncDevices = MutableLiveData<BluetoothDevice>()

    var selectedBluettothDeviceList = ArrayList<BluetoothDevice>()

    var bleKey: String = ""

    var bleName: String = "No Device Found"

    var bleAddress = "No Device Found"

    var gattServiceIntent: Intent? = null

    var selectedGattLiveData: MutableLiveData<BluetoothGatt> = MutableLiveData()

    var mPreSelectedDevice = ""

    var mPreSelectedDeviceBleObject = MutableLiveData<BluetoothDevice>()

    var mAllCharacteristic = MutableLiveData<List<BluetoothGattCharacteristic>>()

    fun setOnClickOfBluetoothDevice(key: String, bluetoothDevice: BluetoothDevice) {

        bleKey = key

        Log.e("bleKey", "setOnClickOfBluetoothDevice: $bleKey")

        selectedBluettothDeviceList.add(bluetoothDevice)

//        bleName = bluetoothDevice.name

        bleAddress = bluetoothDevice.address

        if (bluetoothService == null) {
            startBLEService(PromptBLE.context!!)
        } else {
            bluetoothService = bluetoothService!!.LocalBinder().getService()
            bluetoothService?.let { bluetooth ->
                if (!bluetooth.initialize()) {
                    DLog.e("TAG", "Unable to initialize Bluetooth")
                    //finish()
                }
                bluetooth.connect(selectedBluettothDeviceList.last().address)
            }
        }
    }

    fun setCharacteristics(key: String, rxCharacteristic: String, txCharacteristic: String) {

        bleKey = key

        gattRxCharacteristicList.add(Pair(bleKey, rxCharacteristic))
        gattTxCharacteristicList.add(Pair(bleKey, txCharacteristic))

    }

    fun startBLEService(context: Context) {
        gattServiceIntent = Intent(context, BluetoothLeService::class.java)
        context.bindService(
            gattServiceIntent!!,
            ScanBLEViewModel.serviceConnection,
            Context.BIND_AUTO_CREATE
        )
    }

    fun connectPreSelectedDevice(key: String, preSelectedDevice: String = "") {
        if (preSelectedDevice.isNotEmpty()) {
            mPreSelectedDevice = preSelectedDevice

            bluetoothAdapter?.let { adapter ->
                try {
                    val device = adapter.getRemoteDevice(mPreSelectedDevice)
                    // connect to the GATT server on the device
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) if (ActivityCompat.checkSelfPermission(
                            PromptBLE.context!!.applicationContext,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return@let
                    }

                    setOnClickOfBluetoothDevice(key, device)

                    //return true
                } catch (exception: IllegalArgumentException) {
                    DLog.w(TAG, "Device not found with provided address.  Unable to connect.")
                    //return false
                }
            } ?: run {
                DLog.w(TAG, "BluetoothAdapter not initialized")
                //return false
            }

            return
        }
    }

    fun scanBLEDevices(preSelectedDevice: String = "") {
        if (preSelectedDevice.isNotEmpty() && preSelectedDevice != mPreSelectedDevice) {
            mPreSelectedDevice = preSelectedDevice

            bluetoothAdapter?.let { adapter ->
                try {
                    val device = adapter.getRemoteDevice(mPreSelectedDevice)
                    // connect to the GATT server on the device
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) if (ActivityCompat.checkSelfPermission(
                            PromptBLE.context!!.applicationContext,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        return@let
                    }

                    mPreSelectedDeviceBleObject.postValue(device)

                    //return true
                } catch (exception: IllegalArgumentException) {
                    DLog.w(TAG, "Device not found with provided address.  Unable to connect.")
                    //return false
                }
            } ?: run {
                DLog.w(TAG, "BluetoothAdapter not initialized")
                //return false
            }

            return
        }
        ScanBLEViewModel.scanLeDevice()
    }

    fun stopBLEScan() {
        ScanBLEViewModel.stopScan()
    }

    fun startGattServices() {
        ScanBLEViewModel.displayGattServices(
            bluetoothService!!.getSupportedGattServices(
                selectedGattList.last().second
            ), selectedGattList.last().second
        )
    }

    fun exchangeMTU() {
        //ScanBLEViewModel.exchangeMTU(bleGatt)
        ScanBLEViewModel.exchangeMTU(selectedGattList.last().second)
    }

    fun getDeviceList(): ArrayList<BluetoothDevice> {
        return allDeviceList
    }

    fun getCharacteristicsList(
        key: String,
        preSelectedDevice: String = ""
    ) {
        connectPreSelectedDevice(key, preSelectedDevice)
    }

    fun updateCharacteristics(
        mac: String,
        readCharacteristics: String,
        writeCharacteristics: String
    ) {
        try {
            PromptBLE.storageManager!!.updateFile(
                Utils.fileName,
                "$mac,$readCharacteristics,$writeCharacteristics"
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getServicesList() {

    }

    fun writeData(key: String, value: ByteArray) {
        ScanBLEViewModel.writeData(key, value)
    }

    fun tare(key: String, tare: Int) {
        writeData(key, tare.to2ByteArray())
    }

    fun disconnectAll() {
        mAllCharacteristic.value = null
        if (gattServiceIntent != null) {
//            mPreSelectedDeviceBleObject.postValue(null)
            selectedGattFlow.value = Pair("", BluetoothLeService.tempBleGatt)
            selectedGattList.clear()
//            mPreSelectedDeviceBleObject = MutableLiveData<BluetoothDevice>()
            PromptBLE.context?.stopService(gattServiceIntent)
            gattServiceIntent = null
            mutableDisconnectedBLE.postValue(null)
        }
    }

    fun Int.to2ByteArray(): ByteArray = byteArrayOf(toByte(), shr(8).toByte())


}