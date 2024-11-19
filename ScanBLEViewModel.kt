package com.example.multiblecommunication

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.ComponentName
import android.content.Context
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "BluetoothLeService"

@HiltViewModel
class ScanBLEViewModel @Inject constructor(private val context: Context) : ViewModel() {

    private val easy5Ble = "BB:A0:50:46:A7:5F"

    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanning = false

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000
    private var deviceSet: MutableSet<BluetoothDevice> = mutableSetOf()
    private var selectedDevice: BluetoothDevice? = null

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    var bluetoothService: BluetoothLeService? = null

    var mutableSelectedDevice = MutableLiveData<BluetoothDevice>()

    fun scanLeDevice() {

        bluetoothLeScanner = bluetoothManager.adapter.bluetoothLeScanner

        if (!scanning) { // Stops scanning after a pre-defined scan period.
            viewModelScope.launch {
                delay(SCAN_PERIOD)
                scanning = false
                if (ActivityCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.BLUETOOTH_SCAN
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return@launch
                }
                bluetoothLeScanner!!.stopScan(leScanCallback)
                onClickOfDevice()
            }
            scanning = true
            bluetoothLeScanner!!.startScan(leScanCallback)
        } else {
            scanning = false
            bluetoothLeScanner!!.stopScan(leScanCallback)
            onClickOfDevice()
        }
    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            if (ActivityCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }

            if (callbackType != ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
                // Should not happen.
                return
            }
            result.scanRecord ?: return

            if (!deviceSet.contains(result.device)) {
                deviceSet.add(result.device)
                //Log.e("DEVICE NAME = = = ", result.device.name + " - " + result.device.address)
                if (result.device.address == easy5Ble) {
                    selectedDevice = result.device
                }
            }

        }
    }

    private fun onClickOfDevice() {
        mutableSelectedDevice.postValue(selectedDevice)
    }

    val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            componentName: ComponentName,
            service: IBinder
        ) {
            bluetoothService = (service as BluetoothLeService.LocalBinder).getService()
            bluetoothService?.let { bluetooth ->
                if (!bluetooth.initialize()) {
                    Log.e(TAG, "Unable to initialize Bluetooth")
                    //finish()
                }
                bluetooth.connect(selectedDevice!!.address)
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            bluetoothService = null
        }
    }

    // Demonstrates how to iterate through the supported GATT
    // Services/Characteristics.
    // In this sample, we populate the data structure that is bound to the
    // ExpandableListView on the UI.
    fun displayGattServices(gattServices: List<BluetoothGattService?>?) {
        if (gattServices == null) return
        var uuid: String?
        val gattServiceData: MutableList<HashMap<String, String>> = mutableListOf()
        val gattCharacteristicData: MutableList<ArrayList<HashMap<String, String>>> =
            mutableListOf()
        var mGattCharacteristics: List<BluetoothGattCharacteristic> = mutableListOf()

        // Loops through available GATT Services.
        gattServices.forEach { gattService ->
            val currentServiceData = HashMap<String, String>()
            uuid = gattService!!.uuid.toString()
            currentServiceData["LIST_NAME"] = gattService.toString()
            currentServiceData["LIST_UUID"] = uuid!!
            gattServiceData += currentServiceData

            val gattCharacteristicGroupData: ArrayList<HashMap<String, String>> = arrayListOf()
            val gattCharacteristics = gattService.characteristics
            val charas: MutableList<BluetoothGattCharacteristic> = mutableListOf()

            // Loops through available Characteristics.
            gattCharacteristics.forEach { gattCharacteristic ->
                charas += gattCharacteristic
                val currentCharaData: HashMap<String, String> = hashMapOf()
                uuid = gattCharacteristic.uuid.toString()
                currentCharaData["LIST_NAME"] = gattService.toString()
                currentCharaData["LIST_UUID"] = uuid!!
                gattCharacteristicGroupData += currentCharaData
                if (gattService.uuid.toString() == "0003cdd0-0000-1000-8000-00805f9b0131") {
                    if (gattCharacteristic.uuid.toString() == "0003cdd2-0000-1000-8000-00805f9b0131") {
                        //PROPERTY_NOTIFY
                        //BluetoothLeService.mutableRXChar.postValue(gattCharacteristic)
                    } else if (gattCharacteristic.uuid.toString() == "0003cdd1-0000-1000-8000-00805f9b0131") {
                        //PROPERTY_WRITE_NO_RESPONSE
                        BluetoothLeService.selectedBluetoothGattCharacteristic = gattCharacteristic
                        BluetoothLeService.mutableBleStatusUpdate.postValue(BluetoothLeService.ACTION_MTU_EXCHANGE)
                    }
                    //Log.e("Characteristic - - - ", gattService.uuid.toString())

                }
            }
            mGattCharacteristics += charas
            gattCharacteristicData += gattCharacteristicGroupData

        }
    }

    fun exchangeMTU(){
        bluetoothService!!.exchangeMTU()
    }


}