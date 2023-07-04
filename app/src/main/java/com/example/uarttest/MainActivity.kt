package com.example.uarttest

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues.TAG
import android.content.Context
import android.content.Context.USB_SERVICE
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.Modifier

import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.ViewModelProvider
import com.example.uarttest.viewmodel.SerialViewModel

class MainActivity : ComponentActivity() {

    private val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
    private lateinit var viewModel: SerialViewModel
    private val usbManager by lazy { getSystemService(USB_SERVICE) as UsbManager }
    private var selectedDevice: UsbDevice? = null

    private val usbReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            if (ACTION_USB_PERMISSION == intent.action) {
                synchronized(this) {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        selectedDevice?.apply {
                            //call method to set up device communication
                            viewModel.selectDevice(usbManager, selectedDevice!!)
                        }
                    } else {
                        Log.d(TAG, "permission denied for device $selectedDevice")
                    }
                }
            }
        }
    }


    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this)[SerialViewModel::class.java]
        selectedDevice =  usbManager.deviceList.values.firstOrNull()

        val filter = IntentFilter(ACTION_USB_PERMISSION)
        registerReceiver(usbReceiver, filter)

        setContent {
            val devices: List<UsbDevice> by viewModel.usbDevices.observeAsState(emptyList())
            var expanded by remember { mutableStateOf(false) }
            val dataReceived: String by viewModel.dataReceived.observeAsState("대기 중")
            val sendData = remember { mutableStateOf("") }

            var textFiledSize by remember {
                mutableStateOf(Size.Zero)
            }
            var icon = if(expanded){
                Icons.Default.KeyboardArrowUp
            }else{
                Icons.Default.KeyboardArrowDown
            }

            var deviceName = selectedDevice?.deviceName ?: "Empty Device"

            Column(modifier = Modifier.padding(10.dp)) {
                OutlinedTextField(
                    value = deviceName,
                    readOnly = true,
                    onValueChange = { deviceName = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned {
                            textFiledSize = it.size.toSize()
                        },
                    label = {
                        Text("Select Item")
                    },
                    trailingIcon = {
                        Icon(icon, "", Modifier.clickable { expanded = !expanded })
                    })

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .width(with(LocalDensity.current){
                            textFiledSize.width.toDp()
                        })) {

                    devices.forEach { device ->
                        DropdownMenuItem(
                            text = {
                                Text(text = device.deviceName)
                            },
                            onClick = {
                                expanded = false
                                selectedDevice = device
                            })
                    }
                }

                Button(onClick = { selectedDevice?.let {
                    requestUsbPermission(applicationContext, it)
                    //viewModel.selectDevice(usbManager, it)
                }
                }) {
                    Text("Connect")
                }
                Text("Received data: $dataReceived")
                TextField(
                    value = sendData.value,
                    onValueChange = { sendData.value = it },
                    label = { Text("Data to send") }
                )
                Button(onClick = {
                    viewModel.sendData(sendData.value) }) {
                    Text("Send data")
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDevices(usbManager)
    }

    private fun requestUsbPermission(context: Context, usbDevice: UsbDevice){
        val permissionIntent = PendingIntent.getBroadcast(
            context,
            0,
            Intent(ACTION_USB_PERMISSION),
            PendingIntent.FLAG_MUTABLE
        )
        usbManager.requestPermission(usbDevice, permissionIntent)
    }
}