package com.example.uarttest.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.example.uarttest.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController){
    val dataReceived: String by viewModel.dataReceived.observeAsState("대기 중")
    val sendData = remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(500.dp)
                .padding(10.dp)
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(500.dp)) {
                Text("$dataReceived", Modifier.align(Alignment.Center))
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .padding(10.dp)
        ) {
            Row(modifier = Modifier.padding(10.dp)) {
                TextField(
                    modifier = Modifier.width(200.dp),
                    value = sendData.value,
                    onValueChange = { sendData.value = it },
                    label = { Text("Data to send") }
                )
                Spacer(modifier = Modifier.padding(10.dp))
                Button(
                    onClick = {
                        viewModel.sendData(sendData.value)
                }) {
                    Text("Send")
                }
            }
        }
    }

}

@Preview(showBackground = true)
@Composable
fun preview() {
    HomeScreen(rememberNavController())
}