package com.eksikci.android.network

import android.util.Log
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URISyntaxException
import com.eksikci.android.network.SocketEventListener

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED
}

class SocketManager(private val listener: SocketEventListener, private val coroutineScope: CoroutineScope) {
    private var socket: Socket? = null
    private var isConnecting = false
    private var serverUrl: String? = null
    private var deviceId: String = ""
    private var deviceName: String = ""
    
    // Ping ve bağlantı zaman aşımı için coroutine jobları
    private var pingJob: Job? = null
    private var connectionJob: Job? = null
    
    // Bağlantı durumu için StateFlow
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    companion object {
        private const val TAG = "SocketManager"
    }
    
    fun connect(serverUrl: String? = null, deviceId: String = "", deviceName: String = "") {
        if (isConnecting) {
            Log.d(TAG, "Bağlantı zaten devam ediyor")
            return
        }
        
        if (!serverUrl.isNullOrEmpty()) {
            this.serverUrl = serverUrl
        }
        
        if (deviceId.isNotEmpty()) {
            this.deviceId = deviceId
        }
        
        if (deviceName.isNotEmpty()) {
            this.deviceName = deviceName
        }
        
        if (this.serverUrl.isNullOrEmpty()) {
            listener.onError("Sunucu adresi boş olamaz")
            return
        }
        
        if (this.deviceId.isEmpty()) {
            listener.onError("Cihaz ID boş olamaz")
            return
        }
        
        isConnecting = true
        _connectionState.value = ConnectionState.CONNECTING
        
        try {
            // Önceki socket varsa temizle
            cleanup()
            
            // Socket.IO bağlantı seçenekleri
            val options = IO.Options().apply {
                transports = arrayOf("websocket", "polling")
                reconnection = true
                reconnectionAttempts = 10 // Yeniden bağlanma deneme sayısı arttırıldı
                reconnectionDelay = 1000
                reconnectionDelayMax = 5000
                timeout = 20000 // 20 saniye
                forceNew = false // Aynı URL için mevcut bağlantıyı yeniden kullan
                upgrade = true // Daha iyi protokole yükseltmeyi dene
                rememberUpgrade = true // Bağlantı yükseltmesini hatırla
            }
            
            // Socket.IO istemcisini oluştur
            socket = IO.socket(this.serverUrl, options)
            
            // Socket event listener'ları ayarla
            setupSocketListeners()
            
            // Bağlantıyı başlat
            socket?.connect()
            
            // Bağlantı zaman aşımı kontrolünü başlat
            startConnectionTimeout()
            
        } catch (e: URISyntaxException) {
            isConnecting = false
            _connectionState.value = ConnectionState.DISCONNECTED
            val errorMsg = "Bağlantı hatası: ${e.message}"
            Log.e(TAG, errorMsg)
            listener.onError(errorMsg)
        }
    }
    
    fun isConnected(): Boolean {
        return socket?.connected() == true
    }
    
    fun disconnect() {
        Log.d(TAG, "Socket bağlantısı kapatılıyor")
        cleanup()
        socket?.disconnect()
        socket = null
        _connectionState.value = ConnectionState.DISCONNECTED
    }
    
    fun updateDeviceName(deviceId: String, deviceName: String) {
        if (socket?.connected() == true) {
            this.deviceName = deviceName
            val data = JSONObject().apply {
                put("device_id", deviceId)
                put("name", deviceName)
            }
            socket?.emit("update_device_name", data)
            Log.d(TAG, "Cihaz adı güncellendi: $deviceName")
        } else {
            Log.e(TAG, "Cihaz adı güncellenemedi: Socket bağlı değil")
        }
    }
    
    fun acceptNotification(notificationId: Int) {
        if (socket?.connected() == true) {
            val data = JSONObject().apply {
                put("notification_id", notificationId)
                put("device_id", deviceId)
                put("device_name", deviceName)
            }
            socket?.emit("accept_notification", data)
            Log.d(TAG, "Bildirim kabul edildi - ID: $notificationId")
        } else {
            Log.e(TAG, "Bildirim kabul edilemedi: Socket bağlı değil")
        }
    }
    
    fun completeNotification(notificationId: Int) {
        if (socket?.connected() == true) {
            val data = JSONObject().apply {
                put("notification_id", notificationId)
                put("device_id", deviceId)
            }
            socket?.emit("complete_notification", data)
            Log.d(TAG, "Bildirim tamamlandı - ID: $notificationId")
        } else {
            Log.e(TAG, "Bildirim tamamlanamadı: Socket bağlı değil")
        }
    }
    
    fun sendNotificationStatus(notificationId: Int, status: String) {
        if (socket?.connected() == true) {
            val data = JSONObject().apply {
                put("notification_id", notificationId)
                put("device_id", deviceId)
                put("status", status)
            }
            socket?.emit("notification_status", data)
            Log.d(TAG, "Bildirim durumu gönderildi - ID: $notificationId, Durum: $status")
        } else {
            Log.e(TAG, "Bildirim durumu gönderilemedi: Socket bağlı değil")
        }
    }
    
    fun sendNotificationUpdate(orderNumber: String, status: String) {
        try {
            val data = JSONObject().apply {
                put("orderNumber", orderNumber)
                put("status", status)
                put("deviceId", deviceId)
                put("deviceName", deviceName)
            }
            
            Log.d(TAG, "Bildirim güncelleme gönderiliyor: $data")
            socket?.emit("notification_update", data)
        } catch (e: Exception) {
            Log.e(TAG, "Bildirim güncelleme gönderilirken hata oluştu: ${e.message}")
        }
    }
    
    private fun setupSocketListeners() {
        socket?.let { socket ->
            socket.on(Socket.EVENT_CONNECT) {
                Log.d(TAG, "Socket.IO sunucusuna bağlandı")
                isConnecting = false
                _connectionState.value = ConnectionState.CONNECTED
                
                // Cihaz kaydını gönder
                registerDevice()
                
                // Ping göndermeyi başlat
                startPinging()
                
                // Bağlantı zaman aşımı kontrolünü iptal et
                connectionJob?.cancel()
                
                // Listener'a bildir
                coroutineScope.launch(Dispatchers.Main) {
                    listener.onConnected()
                }
            }
            
            socket.on(Socket.EVENT_DISCONNECT) { args ->
                val reason = if (args.isNotEmpty() && args[0] is String) args[0] as String else "unknown"
                Log.e(TAG, "Socket.IO bağlantısı kesildi: $reason")
                isConnecting = false
                _connectionState.value = ConnectionState.DISCONNECTED
                
                // Ping göndermeyi durdur
                pingJob?.cancel()
                
                // Listener'a bildir
                coroutineScope.launch(Dispatchers.Main) {
                    listener.onDisconnected()
                    
                    // "io server disconnect" hatası geldiğinde otomatik olarak yeniden bağlanma
                    if (reason == "io server disconnect") {
                        Log.d(TAG, "Sunucu tarafından bağlantı kesildi, 3 saniye sonra yeniden bağlanılacak")
                        delay(3000)
                        connect()
                    }
                }
            }
            
            socket.on(Socket.EVENT_CONNECT_ERROR) { args ->
                val error = if (args.isNotEmpty()) args[0].toString() else "unknown"
                Log.e(TAG, "Socket.IO bağlantı hatası: $error")
                isConnecting = false
                _connectionState.value = ConnectionState.DISCONNECTED
                
                // Listener'a bildir
                coroutineScope.launch(Dispatchers.Main) {
                    listener.onError("Bağlantı hatası: $error")
                }
            }
            
            socket.on("new_notification") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val notification = args[0] as JSONObject
                    Log.d(TAG, "Yeni bildirim alındı: $notification")
                    
                    // Listener'a bildir
                    coroutineScope.launch(Dispatchers.Main) {
                        listener.onNewNotification(notification)
                    }
                }
            }
            
            socket.on("notification_accepted") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val data = args[0] as JSONObject
                    val notificationId = data.optInt("notification_id")
                    val deviceId = data.optString("device_id")
                    val deviceName = data.optString("device_name")
                    
                    Log.d(TAG, "Bildirim kabul edildi - ID: $notificationId, Cihaz: $deviceId, Adı: $deviceName")
                    
                    // Listener'a bildir
                    coroutineScope.launch(Dispatchers.Main) {
                        listener.onNotificationAccepted(notificationId, deviceId, deviceName)
                    }
                }
            }
            
            socket.on("notification_completed") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val data = args[0] as JSONObject
                    val notificationId = data.optInt("notification_id")
                    
                    Log.d(TAG, "Bildirim tamamlandı - ID: $notificationId")
                    
                    // Listener'a bildir
                    coroutineScope.launch(Dispatchers.Main) {
                        listener.onNotificationCompleted(notificationId)
                    }
                }
            }
            
            socket.on("notification_updated") { args ->
                if (args.isNotEmpty() && args[0] is JSONObject) {
                    val data = args[0] as JSONObject
                    val orderNumber = data.optString("orderNumber")
                    val status = data.optString("status")
                    val deviceId = data.optString("deviceId")
                    val deviceName = data.optString("deviceName")
                    
                    Log.d(TAG, "Bildirim güncellendi - Sipariş: $orderNumber, Durum: $status, Cihaz: $deviceId, Ad: $deviceName")
                    
                    // Listener'a bildir
                    coroutineScope.launch(Dispatchers.Main) {
                        listener.onNotificationUpdated(orderNumber, status, deviceId, deviceName)
                    }
                }
            }
        }
    }
    
    private fun registerDevice() {
        if (socket?.connected() == true) {
            Log.d(TAG, "Cihaz kaydı gönderiliyor - ID: $deviceId, Ad: $deviceName")
            val data = JSONObject().apply {
                put("device_id", deviceId)
                put("name", deviceName)
            }
            socket?.emit("register_device", data)
        }
    }
    
    private fun startPinging() {
        pingJob?.cancel()
        pingJob = coroutineScope.launch(Dispatchers.IO) {
            while (isActive && socket?.connected() == true) {
                try {
                    delay(10000)  // 10 saniyede bir ping gönder
                    if (socket?.connected() == true) {
                        val pingData = JSONObject().apply {
                            put("device_id", deviceId)
                            put("name", deviceName)
                            put("timestamp", System.currentTimeMillis())
                        }
                        socket?.emit("ping_server", pingData)
                        Log.d(TAG, "Ping gönderildi")
                    } else {
                        Log.e(TAG, "Ping gönderilemedi: Socket bağlı değil")
                        break
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Ping gönderilirken hata: ${e.message}")
                }
            }
        }
    }
    
    private fun startConnectionTimeout() {
        connectionJob?.cancel()
        connectionJob = coroutineScope.launch(Dispatchers.IO) {
            delay(15000)  // 15 saniye bağlantı zaman aşımı
            if (socket?.connected() != true) {
                val errorMsg = "Bağlantı zaman aşımı (15 saniye)"
                Log.e(TAG, errorMsg)
                isConnecting = false
                _connectionState.value = ConnectionState.DISCONNECTED
                socket?.disconnect()
                
                // Listener'a bildir
                coroutineScope.launch(Dispatchers.Main) {
                    listener.onError(errorMsg)
                }
            }
        }
    }
    
    private fun cleanup() {
        pingJob?.cancel()
        pingJob = null
        
        connectionJob?.cancel()
        connectionJob = null
        
        // Mevcut socket varsa event listener'ları temizle ve kapat
        socket?.off()
    }
} 