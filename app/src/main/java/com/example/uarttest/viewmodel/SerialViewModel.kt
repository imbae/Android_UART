package com.example.uarttest.viewmodel

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.felhr.usbserial.UsbSerialDevice
import com.felhr.usbserial.UsbSerialInterface

class SerialViewModel : ViewModel() {
    private var serialPort: UsbSerialDevice? = null
    private var connection: UsbDeviceConnection? = null
    val dataReceived = MutableLiveData<String>()
    val usbDevices = MutableLiveData<List<UsbDevice>>()

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
