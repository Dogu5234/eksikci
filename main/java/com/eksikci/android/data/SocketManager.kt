package com.eksikci.android.data

import android.content.Context
import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import org.json.JSONObject
import java.net.URISyntaxException

class SocketManager private constructor(context: Context) {
    private val TAG = "SocketManager"
    private var socket: Socket? = null
    private val sharedPreferencesManager = SharedPreferencesManager(context)

    init {
        setupSocket()
    }

    private fun setupSocket() {
        val serverUrl = sharedPreferencesManager.getServerUrl()
        if (serverUrl.isNotEmpty()) {
            try {
                val options = IO.Options()
                socket = IO.socket(serverUrl, options)
                socket?.connect()
                setupListeners()
            } catch (e: URISyntaxException) {
                Log.e(TAG, "Socket bağlantı hatası: ${e.message}")
            }
        }
    }

    private fun setupListeners() {
        socket?.on(Socket.EVENT_CONNECT) {
            Log.d(TAG, "Socket bağlandı")
        }

        socket?.on(Socket.EVENT_DISCONNECT) {
            Log.d(TAG, "Socket bağlantısı kesildi")
        }

        socket?.on(Socket.EVENT_CONNECT_ERROR) {
            Log.e(TAG, "Socket bağlantı hatası: ${it.contentToString()}")
        }
    }

    fun reconnect() {
        socket?.disconnect()
        setupSocket()
    }

    fun sendNotificationUpdate(orderNumber: String, status: String) {
        try {
            val data = JSONObject().apply {
                put("orderNumber", orderNumber)
                put("status", status)
                put("deviceId", sharedPreferencesManager.getDeviceId())
                put("deviceName", sharedPreferencesManager.getDeviceName())
            }
            Log.d(TAG, "Bildirim güncelleniyor: $data")
            socket?.emit("notification_update", data)
        } catch (e: Exception) {
            Log.e(TAG, "Bildirim güncellenirken hata oluştu: ${e.message}")
        }
    }

    companion object {
        @Volatile
        private var instance: SocketManager? = null

        fun getInstance(context: Context): SocketManager {
            return instance ?: synchronized(this) {
                instance ?: SocketManager(context.applicationContext).also { instance = it }
            }
        }
    }
} 