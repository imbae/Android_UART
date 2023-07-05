package com.example.uarttest.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.uarttest.screens.HomeScreen
import com.example.uarttest.screens.SettingScreen

@Composable
fun UartNavigation(navHostController: NavHostController) {
    NavHost(
        navController = navHostController,
        startDestination = UartScreens.HomeScreen.name
    ) {
        composable(UartScreens.HomeScreen.name) {
            HomeScreen(navController = navHostController)
        }
        composable(UartScreens.SettingScreen.name) {
            SettingScreen(navController = navHostController)
        }
    }
}