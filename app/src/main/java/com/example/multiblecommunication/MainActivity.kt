package com.example.multiblecommunication

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.multiblecommunication.databinding.ActivityMainBinding
import com.prompt.multiblecommunication.DLog
import com.prompt.multiblecommunication.PromptBLE
import com.prompt.multiblecommunication.PromptUtils
import com.prompt.multiblecommunication.Utils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

var pos: Int = 1

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: BluetoothDevicesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        recyclerSetup()
        initLibrary()
        buttonClickEvent()
        devicesObserver()
        bleEventObserver()
    }

    private fun recyclerSetup() {
        binding.recyclerview.layoutManager = LinearLayoutManager(this)
        adapter = BluetoothDevicesAdapter()
        binding.recyclerview.adapter = adapter
    }

    private fun initLibrary() {
        PromptBLE.init(applicationContext)
    }

    private fun buttonClickEvent() {
        binding.btnScan.setOnClickListener {
            adapter.clearList()
            PromptUtils.scanBLEDevices()
        }
        binding.btnWrite1.setOnClickListener {
            PromptUtils.writeData("BLE1", "BBLE1".toByteArray())
        }
        binding.btnWrite2.setOnClickListener {
            PromptUtils.writeData("BLE2", "BLE2".toByteArray())
        }
        binding.btnWrite3.setOnClickListener {
            PromptUtils.writeData("BLE3", "BLE3".toByteArray())
        }

        binding.btnF20.setOnClickListener {
            PromptUtils.writeData(
                "BLE2",
                "^F20{1:1,2400,N,8,1,9,76,84,1,0,0,0,0 ;2:2,2400,N,8,1,31,40,84,3,0,0,0,0 ;3:3,9600,N,8,1,20,40,32,1,0,0,0,0 ;4:4,9600,N,8,1,10,10,10,1,0,0,0,0 ;}$".toByteArray()
            )
        }
        binding.btnF30.setOnClickListener {
            PromptUtils.writeData("BLE2", "^F30{0,1,0,0,1,1,1,1}$".toByteArray())
        }
        binding.btnF40.setOnClickListener {
            PromptUtils.writeData(
                "BLE2",
                "^F40{1:3,2,6,0,100;2:0,1,3,0,10;2:1,5,3,0,10;2:2,13,4,0,100;}$".toByteArray()
            )
        }
    }

    private fun devicesObserver() {
        PromptUtils.asyncDevices.observeForever {
            adapter.setList(it)
        }
    }

    private fun bleEventObserver() {
        lifecycleScope.launch {
            PromptUtils.selectedGattFlow.collectLatest {
                when (it.first) {
                    PromptUtils.ACTION_GATT_CONNECTED -> {
                        pos++
                        DLog.e("BLE - - - ", "ACTION_GATT_CONNECTED")
                    }

                    PromptUtils.ACTION_GATT_DISCONNECTED -> {
                        DLog.e("BLE - - - ", "ACTION_GATT_DISCONNECTED")
                    }

                    PromptUtils.ACTION_DATA_AVAILABLE -> {

                    }
                }
            }
        }

        PromptUtils.mutableDataFromBLE.observeForever {
            DLog.e("BLE Data - - - ", it.second)
        }
    }


}