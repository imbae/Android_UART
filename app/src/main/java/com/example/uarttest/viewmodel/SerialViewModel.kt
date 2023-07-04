package com.example.uarttest.viewmodel

import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Context.USB_SERVICE
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.util.Log
import androidx.core.app.ComponentActivity
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface

class SerialViewModel(context: Context) : ViewModel() {

    // Singleton instance
    companion object {
        private var instance: SerialViewModel? = null
        fun getInstance(context: Context) = instance ?: synchronized(this) {
            instance ?: SerialViewModel(context).also { instance = it }
        }
    }

    private var serialPort: UsbSerialDevice? = null
    private var connection: UsbDeviceConnection? = null
    val dataReceived = MutableLiveData<String>()
    val usbDevices = MutableLiveData<List<UsbDevice>>()
    var selectedDevice: UsbDevice? = null
    val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
    val usbManager by lazy { context.getSystemService(USB_SERVICE) as UsbManager }

    val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        selectedDevice?.apply {
                            //call method to set up device communication
                            selectDevice(usbManager, selectedDevice!!)
                        }
                    } else {
                        Log.d(ContentValues.TAG, "permission denied for device $selectedDevice")
                    }
                }
            }
        }
    }

    fun refreshDevices(usbManager: UsbManager) {
        val devices: List<UsbDevice> = usbManager.deviceList.values.toList().filter {
            UsbSerialDevice.isSupported(it)
        }

        usbDevices.postValue(devices)
    }

    fun selectDevice(usbManager: UsbManager, device: UsbDevice) {
        val deviceConnection = usbManager.openDevice(device)
        val serialPort = UsbSerialDevice.createUsbSerialDevice(device, deviceConnection)
        if (serialPort != null) {
            if (serialPort.open()) {
                serialPort.setBaudRate(115200)
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

    private val mCallback = UsbSerialInterface.UsbReadCallback { arg0 ->
        val data = String(arg0)
        dataReceived.postValue(data)
    }

    fun sendData(data: String) {
        serialPort?.write(data.toByteArray())
    }
}
