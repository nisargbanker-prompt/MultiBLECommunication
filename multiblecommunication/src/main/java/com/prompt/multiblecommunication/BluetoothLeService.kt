package com.prompt.multiblecommunication

import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import com.prompt.multiblecommunication.PromptUtils.ACTION_DATA_AVAILABLE
import com.prompt.multiblecommunication.PromptUtils.bleKey
import com.prompt.multiblecommunication.PromptUtils.selectedBluettothDeviceList
import com.prompt.multiblecommunication.PromptUtils.selectedGattList
import com.prompt.multiblecommunication.PromptUtils.selectedGattFlow
import com.prompt.multiblecommunication.Utils.byteToString
import com.prompt.multiblecommunication.Utils.convertHexToAscci
import java.util.UUID

private const val TAG = "BluetoothLeService"

class BluetoothLeService : Service() {

    private var connectionState = BluetoothProfile.STATE_DISCONNECTED
    private var bluetoothManager: BluetoothManager? = null

    private val binder = LocalBinder()
    //private var bluetoothGatt: BluetoothGatt? = null

    private var connectedDeviceMap: HashMap<String, BluetoothGatt>? = null

    companion object {

        var bluetoothAdapter: BluetoothAdapter? = null

        var selectedBluetoothGattRXCharacteristicList: ArrayList<Pair<String, BluetoothGattCharacteristic>> =
            ArrayList()
        var selectedBluetoothGattTXCharacteristicList: ArrayList<Pair<String, BluetoothGattCharacteristic>> =
            ArrayList()
        val tempBleGatt: BluetoothGatt? = null
    }

    fun initialize(): Boolean {
        if (bluetoothAdapter == null) {
            DLog.e(TAG, "Unable to obtain a BluetoothAdapter.")
            return false
        }
        return true
    }


    override fun onBind(intent: Intent): IBinder {
        bluetoothManager = this.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager!!.adapter

        connectedDeviceMap = HashMap()

        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {

        selectedBluetoothGattRXCharacteristicList.clear()
        selectedBluetoothGattTXCharacteristicList.clear()

        connectedDeviceMap!!.forEach {
            close(it.value)
        }
        return super.onUnbind(intent)
    }


    inner class LocalBinder : Binder() {
        fun getService(): BluetoothLeService {
            return this@BluetoothLeService
        }
    }

    private val bluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {

                DLog.i(TAG, "Connected to GATT server.")

                if (!connectedDeviceMap!!.containsKey(gatt!!.device.address)) {
                    connectedDeviceMap!![gatt.device.address] = gatt
                }

                selectedGattList.add(Pair(bleKey, gatt))
                /* if (selectedGattList.isEmpty()) {
                     Log.e("BLE Callback : ", "isEmpty")
                 } else {
                     selectedGattList.forEach {
                         if (it.first != bleKey) {
                             Log.e("BLE Callback : ", "forEach ${it.first} - $bleKey")
                             selectedGattList.add(Pair(bleKey, gatt))
                         }
                     }
                 }*/

                selectedGattFlow.value = Pair(PromptUtils.ACTION_GATT_CONNECTED, gatt)

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) if (ActivityCompat.checkSelfPermission(
                        this@BluetoothLeService,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                //bluetoothGatt?.discoverServices()
                gatt.discoverServices()

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {

                DLog.i(TAG, "Disconnected from GATT server.")
                if (connectedDeviceMap!!.containsKey(gatt!!.device.address)) {
                    val bluetoothGatt = connectedDeviceMap!![gatt.device.address]
                    bluetoothGatt?.close()
                    connectedDeviceMap!!.remove(gatt.device.address)
                }
                if (selectedBluettothDeviceList.contains(gatt.device)) {
                    selectedBluettothDeviceList.remove(gatt.device)
                }
                selectedGattFlow.value =
                    Pair(PromptUtils.ACTION_GATT_DISCONNECTED, gatt)

                PromptUtils.mutableDisconnectedBLE.postValue(gatt.device.address)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                selectedGattFlow.value =
                    Pair(PromptUtils.ACTION_GATT_SERVICES_DISCOVERED, gatt)

                PromptUtils.startGattServices()

            } else {
                DLog.w(TAG, "onServicesDiscovered received: $status")
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt?, mtu: Int, status: Int) {
            super.onMtuChanged(gatt, mtu, status)
            readCharacteristic(gatt!!)
            writeDesciptor(gatt)
            setCharacteristicNotification(gatt)
            writeCharacteristics(gatt)
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray
        ) {
            super.onCharacteristicChanged(gatt, characteristic, value)
            PromptUtils.mutableDataFromBLE.postValue(
                Pair(
                    gatt.device.address,
                    convertHexToAscci(byteToString(value))
                )
            )

            selectedGattFlow.value = Pair(ACTION_DATA_AVAILABLE, gatt)
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, value, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //broadcastUpdate(ACTION_DATA_AVAILABLE, value)
            }
        }
    }

    fun getSupportedGattServices(gatt: BluetoothGatt): List<BluetoothGattService?>? {
        return gatt.services
    }

    fun connect(address: String) {
        bluetoothAdapter?.let { adapter ->
            try {
                val device = adapter.getRemoteDevice(address)
                // connect to the GATT server on the device
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return@let
                }


                val connectionState: Int =
                    bluetoothManager!!.getConnectionState(device, BluetoothProfile.GATT)

                if (connectionState == BluetoothProfile.STATE_DISCONNECTED) {
                    device.connectGatt(this, false, bluetoothGattCallback)
                } else if (connectionState == BluetoothProfile.STATE_CONNECTED) {
                    // already connected . send Broadcast if needed
                }

                //return true
            } catch (exception: IllegalArgumentException) {
                DLog.w(TAG, "Device not found with provided address.  Unable to connect.")
                //return false
            }
        } ?: run {
            DLog.w(TAG, "BluetoothAdapter not initialized")
            //return false
        }
    }


    fun exchangeMTU(gatt: BluetoothGatt) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        gatt.requestMtu(512)
    }


    fun readCharacteristic(gatt: BluetoothGatt) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        gatt.readCharacteristic(selectedBluetoothGattRXCharacteristicList.last().second)

    }

    fun setCharacteristicNotification(gatt: BluetoothGatt) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        gatt.setCharacteristicNotification(
            selectedBluetoothGattRXCharacteristicList.last().second,
            true
        )

    }

    fun writeCharacteristics(gatt: BluetoothGatt) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        gatt.writeCharacteristic(selectedBluetoothGattRXCharacteristicList.last().second)

    }

    fun writeDesciptor(gatt: BluetoothGatt) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        val descriptor =
            selectedBluetoothGattRXCharacteristicList.last().second.getDescriptor(
                UUID.fromString(
                    selectedBluetoothGattRXCharacteristicList.last().second.descriptors[0].uuid.toString()
                )
            )
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)

        gatt.writeDescriptor(descriptor)

    }

    private fun close(gatt: BluetoothGatt) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        gatt.close()
    }

    fun write(gatt: BluetoothGatt) {
        //gatt.writeCharacteristic()
    }


}