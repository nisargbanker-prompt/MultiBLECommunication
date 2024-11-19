package com.example.multiblecommunication

import android.bluetooth.BluetoothDevice
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.prompt.multiblecommunication.PromptUtils

class BluetoothDevicesAdapter : RecyclerView.Adapter<BluetoothDevicesAdapter.ViewHolder>() {

    private val mBLEList = ArrayList<BluetoothDevice>()

    fun clearList() {
        mBLEList.clear()
        notifyDataSetChanged()
    }

    fun setList(bluetoothDevice: BluetoothDevice) {
        mBLEList.add(bluetoothDevice)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val ble = mBLEList[position]
        holder.textView.text = ble.name + " - " + ble.address

        holder.itemView.setOnClickListener {
            Log.e("Selected BLE device : ", ble.name + " - " + ble.address)
            when (pos) {
                1 -> {

                    PromptUtils.setOnClickOfBluetoothDevice("BLE1", ble)
                }
                2 -> {
                    PromptUtils.setOnClickOfBluetoothDevice("BLE2", ble)
                }
                3 -> {
                    PromptUtils.setOnClickOfBluetoothDevice("BLE3", ble)
                }
                else -> {
                    PromptUtils.setOnClickOfBluetoothDevice("BLE4", ble)
                }
            }
            //2nd
            //PromptUtils.setOnClickOfBluetoothDevice("BLE1", ble)
        }

    }

    override fun getItemCount(): Int {
        return mBLEList.size
    }

    class ViewHolder(ItemView: View) : RecyclerView.ViewHolder(ItemView) {
        val textView: TextView = itemView.findViewById(R.id.txtBleName)
    }
}