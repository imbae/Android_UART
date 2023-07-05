package com.example.uarttest.viewmodel

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Context.USB_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface

class SerialViewModel private constructor(private val context: Context) : ViewModel() {

    // Singleton instance
    companion object {
        private var instance: SerialViewModel? = null
        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: SerialViewModel(context).also { instance = it }
        }
    }

    private val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
    private var serialPort: UsbSerialDevice? = null
    private var connection: UsbDeviceConnection? = null
    private val usbManager by lazy { context.getSystemService(USB_SERVICE) as UsbManager }
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        selectDevice()
                    } else {
                        Log.d(ContentValues.TAG, "permission denied for device $selectedDevice")
                    }
                }
            }
        }
    }

    val dataReceived = MutableLiveData<String>()
    val usbDevices = MutableLiveData<List<UsbDevice>>()
    var selectedDevice: UsbDevice? = null

    val baudrates: MutableLiveData<List<Int>> = MutableLiveData(listOf(57600, 115200))
    var selectedBaudrate: Int? = baudrates.value?.get(0)

    fun initializeViewModel(){
        selectedDevice = usbManager.deviceList.values.firstOrNull()

        val filter = IntentFilter(ACTION_USB_PERMISSION)
        context.registerReceiver(usbReceiver, filter)
    }

    fun refreshDevices() {
        val devices: List<UsbDevice> = usbManager.deviceList.values.toList().filter {
            UsbSerialDevice.isSupported(it)
        }

        usbDevices.postValue(devices)
    }

    fun selectDevice() {
        val deviceConnection = usbManager.openDevice(selectedDevice)
        val serialPort = UsbSerialDevice.createUsbSerialDevice(selectedDevice, deviceConnection)
        if (serialPort != null) {
            if (serialPort.open()) {
                serialPort.setBaudRate(selectedBaudrate!!)
                serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8)
                serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1)
                serialPort.setParity(UsbSerialInterface.PARITY_NONE)
                serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF)
                serialPort.read(mCallback)
                this.serialPort = serialPort
                this.connection = deviceConnection
            }
        }
    }

    fun requestUsbPermission() {
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_MUTABLE
        )
        usbManager.requestPermission(selectedDevice, permissionIntent)
    }

    private val mCallback = UsbSerialInterface.UsbReadCallback { arg0 ->
        val data = String(arg0)
        dataReceived.postValue(data)
    }

    fun sendData(data: String) {
        serialPort?.write(data.toByteArray())
    }
}
