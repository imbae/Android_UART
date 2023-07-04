package com.example.uarttest

import android.annotation.SuppressLint
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.core.content.ContextCompat.getSystemService
import androidx.lifecycle.ViewModelProvider
import com.example.uarttest.viewmodel.SerialViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        val viewModel = SerialViewModel.getInstance(this)

        super.onCreate(savedInstanceState)
        //viewModel = ViewModelProvider(this)[SerialViewModel::class.java]
        viewModel.selectedDevice = viewModel.usbManager.deviceList.values.firstOrNull()

        val filter = IntentFilter(viewModel.ACTION_USB_PERMISSION)
        registerReceiver(viewModel.usbReceiver, filter)

        setContent {
            createMainScaffold(this)
        }
    }



    override fun onResume() {
        super.onResume()
        val viewModel = SerialViewModel.getInstance(this)
        viewModel.refreshDevices(viewModel.usbManager)
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun createSerialConnectContent(context: Context) {
    val viewModel = remember { SerialViewModel.getInstance(context) }

    val devices: List<UsbDevice> by viewModel.usbDevices.observeAsState(emptyList())
    var expanded by remember { mutableStateOf(false) }
    val dataReceived: String by viewModel.dataReceived.observeAsState("대기 중")
    val sendData = remember { mutableStateOf("") }

    var textFiledSize by remember {
        mutableStateOf(Size.Zero)
    }
    var icon = if (expanded) {
        Icons.Default.KeyboardArrowUp
    } else {
        Icons.Default.KeyboardArrowDown
    }

    var deviceName = viewModel.selectedDevice?.deviceName ?: "Empty Device"

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
                .width(with(LocalDensity.current) {
                    textFiledSize.width.toDp()
                })
        ) {

            devices.forEach { device ->
                DropdownMenuItem(
                    text = {
                        Text(text = device.deviceName)
                    },
                    onClick = {
                        expanded = false
                        viewModel.selectedDevice = device
                    })
            }
        }

        Button(onClick = {
            viewModel.selectedDevice?.let {
                requestUsbPermission(context, it)
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
            viewModel.sendData(sendData.value)
        }) {
            Text("Send data")
        }
    }
}

fun requestUsbPermission(context: Context, usbDevice: UsbDevice) {
    val viewModel = SerialViewModel.getInstance(context)
    val permissionIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(viewModel.ACTION_USB_PERMISSION),
        PendingIntent.FLAG_MUTABLE
    )
    viewModel.usbManager.requestPermission(usbDevice, permissionIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun createMainScaffold(context: Context) {
    val openDialog = remember { mutableStateOf(false) }
    Scaffold(
        bottomBar = { createNavigationBar() },
        floatingActionButtonPosition = FabPosition.End,
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    openDialog.value = true
                }
            ) {
                Text("Connect")
            }
        }) { innerPadding ->
        BodyContent(Modifier.padding(innerPadding))

        if (openDialog.value) {
            showConnectDialog(context, openDialog)
        }
    }
}

@Composable
fun createNavigationBar() {
    var selectedItem by remember { mutableStateOf(0) }
    val items = listOf("PILS", "Setting")
    val iconItems = listOf(Icons.Filled.PlayArrow, Icons.Filled.Settings)

    NavigationBar {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(iconItems[index], contentDescription = item) },
                label = { Text(item) },
                selected = selectedItem == index,
                onClick = { selectedItem = index }
            )
        }
    }
}

@Composable
fun showConnectDialog(context: Context, openDialog: MutableState<Boolean>){
    AlertDialog(
        onDismissRequest = {
            // Dismiss the dialog when the user clicks outside the dialog or on the back
            // button. If you want to disable that functionality, simply use an empty
            // onDismissRequest.
            openDialog.value = false
        },
        icon = { Icon(Icons.Filled.Favorite, contentDescription = null) },
        title = {
            Text(text = "Title")
        },
        text = {
            createSerialConnectContent(context)
        },
        confirmButton = {
            TextButton(
                onClick = {

                    openDialog.value = false
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    openDialog.value = false
                }
            ) {
                Text("Dismiss")
            }
        }
    )
}

@Composable
fun BodyContent(modifier: Modifier = Modifier) {

}

@Preview(showBackground = true)
@Composable
fun preview() {
    //createMainScaffold()
}