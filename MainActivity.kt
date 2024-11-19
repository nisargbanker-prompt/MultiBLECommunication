package com.example.multiblecommunication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.multiblecommunication.databinding.ActivityMainBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: ScanBLEViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        viewModel.scanLeDevice()

        viewModel.mutableSelectedDevice.observeForever {
            if (it != null) {
                val gattServiceIntent = Intent(this, BluetoothLeService::class.java)
                bindService(
                    gattServiceIntent,
                    viewModel.serviceConnection,
                    Context.BIND_AUTO_CREATE
                )
            }
        }

        BluetoothLeService.mutableBleStatusUpdate.observeForever {
            when (it) {
                BluetoothLeService.ACTION_GATT_CONNECTED -> {
                    Log.e("BLE - - - ","ACTION_GATT_CONNECTED")
                }
                BluetoothLeService.ACTION_GATT_DISCONNECTED -> {
                    Log.e("BLE - - - ","ACTION_GATT_DISCONNECTED")
                }
                BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED -> {
                    viewModel.displayGattServices(viewModel.bluetoothService!!.getSupportedGattServices())
                }
                BluetoothLeService.ACTION_MTU_EXCHANGE -> {
                    viewModel.exchangeMTU()
                }
            }
        }

        BluetoothLeService.mutableDataFromBLE.observeForever {
            Log.e("BLE Data - - - " , Utils.convertHexToAscci(Utils.byteToString(it)))
        }

    }

}