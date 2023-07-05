package com.example.uarttest.navigation

enum class UartScreens {
    HomeScreen,
    SettingScreen;

    companion object {
        fun fromRoute(route: String?): UartScreens = when (route?.substringBefore("/")){
            HomeScreen.name -> HomeScreen
            SettingScreen.name -> SettingScreen
            null -> HomeScreen
            else -> throw IllegalArgumentException("Route $route is nor recognize")
        }
    }
}