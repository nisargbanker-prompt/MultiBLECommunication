package com.prompt.multiblecommunication

import android.bluetooth.BluetoothGatt
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
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import com.prompt.multiblecommunication.PromptUtils.allDeviceList
import com.prompt.multiblecommunication.PromptUtils.asyncDevices
import com.prompt.multiblecommunication.PromptUtils.bleKey
import com.prompt.multiblecommunication.PromptUtils.bluetoothService
import com.prompt.multiblecommunication.PromptUtils.deviceSet
import com.prompt.multiblecommunication.PromptUtils.scanResults
import com.prompt.multiblecommunication.PromptUtils.selectedGattFlow
import com.prompt.multiblecommunication.PromptUtils.selectedGattList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val TAG = "BluetoothLeService"

internal object ScanBLEViewModel {

    private var bluetoothLeScanner: BluetoothLeScanner? = null
    private var scanning = false

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000

    private val bluetoothManager =
        PromptBLE.context!!.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    fun scanLeDevice() {

        deviceSet.clear()
        PromptUtils.scanResults.clear()

        bluetoothLeScanner = bluetoothManager.adapter.bluetoothLeScanner

        if (!scanning) { // Stops scanning after a pre-defined scan period.
            CoroutineScope(Dispatchers.IO).launch {
                delay(SCAN_PERIOD)
                scanning = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) if (ActivityCompat.checkSelfPermission(
                        PromptBLE.context!!,
                        android.Manifest.permission.BLUETOOTH_SCAN
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return@launch
                }
                stopScan()
            }
            scanning = true
            bluetoothLeScanner!!.startScan(leScanCallback)
            selectedGattFlow.value =
                Pair(PromptUtils.START_SCAN, null)
        } else {
            stopScan()
        }

    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) if (ActivityCompat.checkSelfPermission(
                    PromptBLE.context!!,
                    android.Manifest.permission.BLUETOOTH_SCAN
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }


            val indexQuery = scanResults.indexOfFirst { it.device.address == result.device.address }
            if (indexQuery != -1) { // A scan result already exists with the same address
                scanResults[indexQuery] = result
//                scanResultAdapter.notifyItemChanged(indexQuery)
            } else {
                with(result.device) {

                    if (!deviceSet.contains(result.device) && result.device.name != null) {
                        allDeviceList.add(result.device)
                        deviceSet.add(result.device)
                        asyncDevices.postValue(result.device)

                        if (PromptUtils.mPreSelectedDevice == result.device.address) {
                            stopScan()
                            PromptUtils.mPreSelectedDeviceBleObject.postValue(result.device)
                            allDeviceList.clear()
                            deviceSet.clear()
                        }

                    }
//                    Timber.i("Found BLE device! Name: ${name ?: "Unnamed"}, address: $address")
                }
                scanResults.add(result)
//                scanResultAdapter.notifyItemInserted(scanResults.size - 1)
            }

            if (callbackType != ScanSettings.CALLBACK_TYPE_ALL_MATCHES) {
                // Should not happen.
                return
            }
            result.scanRecord ?: return

            /*asyncDevices.postValue(result.device)
            if (!deviceSet.contains(result.device)) {
                allDeviceList.add(result.device)
                deviceSet.add(result.device)

            }*/
        }
    }

    fun stopScan() {
        scanning = false
        val scanner: BluetoothLeScanner = getScanner()
        if (scanner != null) {
            scanner.stopScan(leScanCallback)
            selectedGattFlow.value =
                Pair(PromptUtils.STOP_SCAN, null)
        }
    }

    private fun getScanner(): BluetoothLeScanner {
        val scanner: BluetoothLeScanner =
            bluetoothManager.adapter.getBluetoothLeScanner()
        if (scanner == null) {
            Log.e("PSF:", "getScanner: cannot get BluetoothLeScanner")
        }
        return scanner
    }

    val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            componentName: ComponentName,
            service: IBinder
        ) {
            bluetoothService = (service as BluetoothLeService.LocalBinder).getService()
            bluetoothService?.let { bluetooth ->
                if (!bluetooth.initialize()) {
                    DLog.e(TAG, "Unable to initialize Bluetooth")
                    //finish()
                }
                //bluetooth.connect(PromptUtils.selectedBluettothDeviceList.last().address)
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
    fun displayGattServices(gattServices: List<BluetoothGattService?>?, gatt: BluetoothGatt) {
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
            val characteristic = ArrayList<BluetoothGattCharacteristic>()

            // Loops through available Characteristics.
            gattCharacteristics.forEach { gattCharacteristic ->
                charas += gattCharacteristic
                val currentCharaData: HashMap<String, String> = hashMapOf()
                uuid = gattCharacteristic.uuid.toString()
                currentCharaData["LIST_NAME"] = gattService.toString()
                currentCharaData["LIST_UUID"] = uuid!!
                gattCharacteristicGroupData += currentCharaData

                characteristic.add(gattCharacteristic)

                //000000ff-0000-1000-8000-00805f9b34fb
                when (gattCharacteristic.properties) {
                    BluetoothGattCharacteristic.PROPERTY_READ -> {}
                    BluetoothGattCharacteristic.PROPERTY_NOTIFY -> {
                        BluetoothLeService.selectedBluetoothGattRXCharacteristicList.add(
                            Pair(
                                bleKey,
                                gattCharacteristic
                            )
                        )
                        PromptUtils.exchangeMTU()
                    }

                    BluetoothGattCharacteristic.PROPERTY_INDICATE -> {
                        BluetoothLeService.selectedBluetoothGattRXCharacteristicList.add(
                            Pair(
                                bleKey,
                                gattCharacteristic
                            )
                        )
                        PromptUtils.exchangeMTU()
                    }

                    BluetoothGattCharacteristic.PROPERTY_WRITE -> {}
                    BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE -> {
                        BluetoothLeService.selectedBluetoothGattTXCharacteristicList.add(
                            Pair(
                                bleKey,
                                gattCharacteristic
                            )
                        )
                        PromptUtils.exchangeMTU()
                    }

                    26 -> {
                        BluetoothLeService.selectedBluetoothGattRXCharacteristicList.add(
                            Pair(
                                bleKey,
                                gattCharacteristic
                            )
                        )
                        BluetoothLeService.selectedBluetoothGattTXCharacteristicList.add(
                            Pair(
                                bleKey,
                                gattCharacteristic
                            )
                        )
                        PromptUtils.exchangeMTU()
                    }

                    else -> {}
                }
                PromptUtils.selectedGattFlow.value =
                    Pair(PromptUtils.ACTION_MTU_EXCHANGE, gatt)

            }
            mGattCharacteristics += charas
            gattCharacteristicData += gattCharacteristicGroupData

            PromptUtils.mAllCharacteristic.postValue(characteristic)

        }
    }

    fun exchangeMTU(gatt: BluetoothGatt) {
        bluetoothService!!.exchangeMTU(gatt)
    }

    fun writeData(key: String, value: ByteArray) {
        val containsGatt =
            selectedGattList.any { it.first == key }

        if (containsGatt) {
            val gatt = selectedGattList.find {
                it.first == key
            }

            val characteristic = BluetoothLeService.selectedBluetoothGattTXCharacteristicList.find {
                it.first == key
            }

            characteristic!!.second.value = value

            gatt!!.second.writeCharacteristic(
                characteristic.second
            )

        }

    }
}
