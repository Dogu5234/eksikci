package com.eksikci.android.data

import android.content.Context
import android.content.SharedPreferences
import java.util.UUID

class SharedPreferencesManager(context: Context) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    companion object {
        private const val PREFS_NAME = "eksikci_preferences"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_NAME = "device_name"
        private const val KEY_SERVER_URL = "server_url"
        private const val DEFAULT_SERVER_URL = "http://192.168.102.237:5000"
        private const val DEFAULT_DEVICE_NAME = "Android Cihaz"
    }
    
    fun getDeviceId(): String {
        var deviceId = prefs.getString(KEY_DEVICE_ID, null)
        if (deviceId == null) {
            deviceId = UUID.randomUUID().toString()
            saveDeviceId(deviceId)
        }
        return deviceId
    }
    
    private fun saveDeviceId(deviceId: String) {
        prefs.edit().putString(KEY_DEVICE_ID, deviceId).apply()
    }
    
    fun getDeviceName(): String {
        return prefs.getString(KEY_DEVICE_NAME, DEFAULT_DEVICE_NAME) ?: DEFAULT_DEVICE_NAME
    }
    
    fun saveDeviceName(deviceName: String) {
        prefs.edit().putString(KEY_DEVICE_NAME, deviceName).apply()
    }
    
    fun getServerUrl(): String {
        return prefs.getString(KEY_SERVER_URL, DEFAULT_SERVER_URL) ?: DEFAULT_SERVER_URL
    }
    
    fun saveServerUrl(serverUrl: String) {
        prefs.edit().putString(KEY_SERVER_URL, serverUrl).apply()
    }
    
    fun saveSettings(deviceName: String, serverUrl: String) {
        prefs.edit()
            .putString(KEY_DEVICE_NAME, deviceName)
            .putString(KEY_SERVER_URL, serverUrl)
            .apply()
    }
} 