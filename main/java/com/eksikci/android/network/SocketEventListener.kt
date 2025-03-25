package com.eksikci.android.network

import org.json.JSONObject

interface SocketEventListener {
    fun onConnected()
    fun onDisconnected()
    fun onError(errorMessage: String)
    fun onNewNotification(notification: JSONObject)
    fun onNotificationAccepted(notificationId: Int, deviceId: String, deviceName: String?)
    fun onNotificationCompleted(notificationId: Int)
    fun onNotificationUpdated(orderNumber: String, status: String, deviceId: String, deviceName: String)
} 