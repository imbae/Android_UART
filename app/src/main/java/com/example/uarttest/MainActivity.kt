package com.example.uarttest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.Modifier

import android.hardware.usb.UsbDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toSize
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.uarttest.navigation.UartNavigation
import com.example.uarttest.viewmodel.SerialViewModel

lateinit var viewModel: SerialViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = SerialViewModel.getInstance(this)
        viewModel.initializeViewModel()

        setContent {
            createMainScaffold()

        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.refreshDevices()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun createSerialConnectContent() {
    val devices: List<UsbDevice> by viewModel.usbDevices.observeAsState(emptyList())
    val baudrates: List<Int> by viewModel.baudrates.observeAsState(emptyList())
    val dataReceived: String by viewModel.dataReceived.observeAsState("대기 중")

    var expanded by remember { mutableStateOf(false) }
    var baudrateExpanded by remember { mutableStateOf(false) }
    var textFiledSize by remember { mutableStateOf(Size.Zero) }
    var icon =
        if (expanded) { Icons.Default.KeyboardArrowUp }
        else { Icons.Default.KeyboardArrowDown }

    var productName = viewModel.selectedDevice?.productName ?: "Empty Device"
    var baudrateName = viewModel.selectedBaudrate?: "Empty Baudrate"

    Column(modifier = Modifier.padding(10.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize(Alignment.TopStart)
        ) {
            OutlinedTextField(
                value = productName,
                readOnly = true,
                onValueChange = { productName = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned {
                        textFiledSize = it.size.toSize()
                    },
                label = {
                    Text("Select Device")
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
                            device.productName?.let { Text(text = it) }
                        },
                        onClick = {
                            expanded = false
                            viewModel.selectedDevice = device
                        })
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentSize(Alignment.TopStart)
        ) {
            OutlinedTextField(
                value = baudrateName.toString(),
                readOnly = true,
                onValueChange = { baudrateName = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned {
                        textFiledSize = it.size.toSize()
                    },
                label = {
                    Text("Select Baud-rate")
                },
                trailingIcon = {
                    Icon(icon, "", Modifier.clickable { baudrateExpanded = !baudrateExpanded })
                })

            DropdownMenu(
                expanded = baudrateExpanded,
                onDismissRequest = { baudrateExpanded = false },
                modifier = Modifier
                    .width(with(LocalDensity.current) {
                        textFiledSize.width.toDp()
                    })
            ) {

                baudrates.forEach { baudrate ->
                    DropdownMenuItem(
                        text = {
                            Text(text = baudrate.toString())
                        },
                        onClick = {
                            baudrateExpanded = false
                            viewModel.selectedBaudrate = baudrate
                        })
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun createMainScaffold() {
    val navController = rememberNavController()
    val openDialog = remember { mutableStateOf(false) }

    Scaffold(
        bottomBar = { createNavigationBar(navController) },
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
        BodyContent(Modifier.padding(innerPadding), navController)

        if (openDialog.value) {
            showConnectDialog(openDialog)
        }
    }
}

@Composable
fun createNavigationBar(navController: NavController) {
    var selectedItem by remember { mutableStateOf(0) }
    val items = listOf("Home", "Setting")
    val iconItems = listOf(Icons.Filled.Home, Icons.Filled.Settings)

    NavigationBar {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                icon = { Icon(iconItems[index], contentDescription = item) },
                label = { Text(item) },
                selected = selectedItem == index,
                onClick = {
                    selectedItem = index
                    navController.navigate(item+"Screen")
                }
            )
        }
    }
}


@Composable
fun showConnectDialog(openDialog: MutableState<Boolean>) {

    AlertDialog(
        onDismissRequest = {
            // Dismiss the dialog when the user clicks outside the dialog or on the back
            // button. If you want to disable that functionality, simply use an empty
            // onDismissRequest.
            openDialog.value = false
        },
        icon = { Icon(Icons.Filled.Search, contentDescription = null) },
        title = {
            Text(text = "Connect Device")
        },
        text = {
            createSerialConnectContent()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.requestUsbPermission()
                    openDialog.value = false
                }
            ) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    openDialog.value = false
                }
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun BodyContent(modifier: Modifier = Modifier, navHostController: NavHostController) {
    UartNavigation(navHostController)
}

@Preview(showBackground = true)
@Composable
fun preview() {
    createMainScaffold()
}