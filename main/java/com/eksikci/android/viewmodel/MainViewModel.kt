package com.eksikci.android.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.eksikci.android.data.AppDatabase
import com.eksikci.android.data.Notification
import com.eksikci.android.data.NotificationProduct
import com.eksikci.android.network.SocketManager
import com.eksikci.android.network.SocketEventListener
import com.eksikci.android.utils.NotificationHelper
import kotlinx.coroutines.*
import org.json.JSONObject
import java.util.*
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope as vmScope

class MainViewModel(application: Application) : AndroidViewModel(application), SocketEventListener {
    private val database = AppDatabase.getDatabase(application)
    private val notificationDao = database.notificationDao()
    private val productDao = database.notificationProductDao()
    private val socketManager = SocketManager(this, vmScope)
    private var deviceId = UUID.randomUUID().toString()
    private var deviceName = ""
    private var serverUrl = "http://192.168.101.79:5002"
    
    // Bildirim yardımcısı
    private val notificationHelper = NotificationHelper(application.applicationContext)

    private val _connectionStatus = MutableLiveData(false)
    val connectionStatus: LiveData<Boolean> = _connectionStatus

    // ConnectionState için LiveData ekleyelim
    private val _connectionState = MutableLiveData(ConnectionState.DISCONNECTED)
    val connectionState: LiveData<ConnectionState> = _connectionState

    private val _errorMessage = MutableLiveData<String?>()
    val errorMessage: LiveData<String?> = _errorMessage

    private val _notifications = MutableLiveData<List<NotificationItem>>()
    val notifications: LiveData<List<NotificationItem>> = _notifications

    init {
        // Başlangıçta bağlantı kurma, bu MainActivity'den yapılacak
        loadNotifications()
        
        // Uygulama başladığında otomatik olarak sunucuya bağlan
        viewModelScope.launch {
            delay(1000) // Uygulamanın başlaması için kısa bir süre bekle
            connectToServer()
        }
    }

    fun setDeviceName(name: String) {
        this.deviceName = name
        if (socketManager.isConnected()) {
            // Eğer zaten bağlıysa, cihaz adını güncelle
            socketManager.updateDeviceName(deviceId, name)
        } else {
            // Bağlı değilse, yeni bağlantı kur
            connectToServer(serverUrl, name)
        }
    }

    fun connectToServer(serverUrl: String? = null, name: String? = null) {
        if (!serverUrl.isNullOrEmpty()) {
            this.serverUrl = serverUrl
        }
        
        if (!name.isNullOrEmpty()) {
            this.deviceName = name
        }
        
        Log.d(TAG, "Sunucuya bağlanılıyor: ${this.serverUrl}, Cihaz ID: $deviceId, Cihaz Adı: ${this.deviceName}")
        // deviceId parametresi zorunlu olduğu için boş kontrolü yapalım
        if (deviceId.isNotEmpty()) {
            // Bağlantı durumunu "CONNECTING" olarak güncelle
            _connectionState.postValue(ConnectionState.CONNECTING)
            socketManager.connect(serverUrl = this.serverUrl, deviceId = deviceId, deviceName = this.deviceName)
        } else {
            Log.e(TAG, "deviceId boş, bağlantı kurulamıyor!")
            _errorMessage.postValue("Cihaz ID boş, bağlantı kurulamıyor!")
            _connectionState.postValue(ConnectionState.DISCONNECTED)
        }
    }

    fun disconnect() {
        socketManager.disconnect()
        // Ses çalma işlemini durdur
        notificationHelper.stopSound()
    }

    override fun onConnected() {
        _connectionStatus.postValue(true)
        _connectionState.postValue(ConnectionState.CONNECTED)
        _errorMessage.postValue(null)
        loadNotifications()
        
        Log.d(TAG, "Sunucuya başarıyla bağlandı")
    }

    override fun onDisconnected() {
        _connectionStatus.postValue(false)
        _connectionState.postValue(ConnectionState.DISCONNECTED)
        Log.d(TAG, "Sunucu bağlantısı kesildi, 5 saniye sonra yeniden bağlanmayı deneyeceğiz")
        
        // Bağlantı kesildiğinde otomatik olarak yeniden bağlanmayı dene
        viewModelScope.launch {
            delay(5000) // 5 saniye bekle
            if (!socketManager.isConnected()) {
                Log.d(TAG, "Yeniden bağlanma denemesi yapılıyor...")
                _errorMessage.postValue("Sunucuya yeniden bağlanılıyor...")
                connectToServer()
            }
        }
    }

    override fun onNotificationAccepted(notificationId: Int, deviceId: String, deviceName: String?) {
        // Başka bir cihaz bildirimi kabul ettiğinde
        Log.d(TAG, "Bildirim kabul edildi - ID: $notificationId, Cihaz: $deviceId, Ad: $deviceName")
        
        // Bildirimler listesini güncelle
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Bildirimi veritabanından bul
                    val notification = notificationDao.getNotificationById(notificationId)
                    if (notification != null) {
                        // Bildirim durumunu güncelle
                        // DAO'nun beklediği parametre isimleri
                        notificationDao.updateStatus(
                            id = notificationId,
                            status = "assigned", 
                            updatedAt = Date().time,
                            completedAt = null,  // tamamlanma tarihi null olmalı
                            assignedDeviceId = deviceId,  // parametre adı değiştirildi
                            assignedDeviceName = deviceName ?: "Bilinmeyen Cihaz"  // parametre adı değiştirildi
                        )
                        
                        Log.d(TAG, "Bildirim durumu veritabanında güncellendi - ID: $notificationId")
                    } else {
                        Log.e(TAG, "Bildirim bulunamadı - ID: $notificationId")
                    }
                }
                
                // Bildirimleri yeniden yükle
                loadNotifications()
            } catch (e: Exception) {
                Log.e(TAG, "Bildirim durumu güncellenirken hata oluştu", e)
                _errorMessage.postValue("Bildirim durumu güncellenirken hata oluştu: ${e.message}")
            }
        }
    }
    
    override fun onNotificationCompleted(notificationId: Int) {
        // Bildirim tamamlandığında
        Log.d(TAG, "Bildirim tamamlandı - ID: $notificationId")
        
        // Bildirimler listesini güncelle
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    // Bildirimi veritabanından bul
                    val notification = notificationDao.getNotificationById(notificationId)
                    if (notification != null) {
                        val now = Date().time
                        // Bildirim durumunu tamamlandı olarak güncelle
                        notificationDao.updateStatus(
                            id = notificationId,
                            status = "completed",
                            updatedAt = now,
                            completedAt = now,
                            assignedDeviceId = notification.assignedDeviceId,
                            assignedDeviceName = notification.assignedDeviceName
                        )
                        
                        // Bildirimi ve ilişkili ürünleri yerel veritabanından sil
                        notificationDao.deleteNotification(notificationId)
                        productDao.deleteProductsForNotification(notificationId)
                        
                        Log.d(TAG, "Bildirim tamamlandı ve veritabanından silindi - ID: $notificationId")
                    } else {
                        Log.e(TAG, "Bildirim bulunamadı - ID: $notificationId")
                    }
                }
                
                // Bildirimleri yeniden yükle
                loadNotifications()
            } catch (e: Exception) {
                Log.e(TAG, "Bildirim tamamlanırken hata oluştu", e)
                _errorMessage.postValue("Bildirim tamamlanırken hata oluştu: ${e.message}")
            }
        }
    }

    override fun onError(error: String) {
        _errorMessage.postValue(error)
        _connectionStatus.postValue(false)
        _connectionState.postValue(ConnectionState.DISCONNECTED)
        
        Log.e(TAG, "Bağlantı hatası: $error")
        
        // Kritik hatalarda yeniden bağlanmayı dene
        if (error.contains("timeout", ignoreCase = true) || 
            error.contains("refused", ignoreCase = true) ||
            error.contains("failed", ignoreCase = true) ||
            error.contains("disconnect", ignoreCase = true) ||
            error.contains("error", ignoreCase = true)) {
            
            Log.d(TAG, "Kritik hata nedeniyle 10 saniye sonra yeniden bağlanmayı deneyeceğiz")
            viewModelScope.launch {
                delay(10000) // 10 saniye bekle
                if (!socketManager.isConnected()) {
                    Log.d(TAG, "Yeniden bağlanma denemesi yapılıyor...")
                    _errorMessage.postValue("Sunucuya yeniden bağlanılıyor...")
                    connectToServer()
                }
            }
        }
    }

    override fun onNewNotification(notification: JSONObject) {
        try {
            // Bildirim sesi çal
            notificationHelper.playNotificationSound()
            
            // Bildirim işleme kodu
            viewModelScope.launch {
                try {
                    // Önce bildirim içindeki ürün sayısını kontrol et ve logla
                    val productsArray = notification.getJSONArray("products")
                    val productCount = productsArray.length()
                    Log.d(TAG, "Bildirim alındı - ID: ${notification.getInt("notification_id")}, Sipariş: ${notification.getString("order_number")}, Ürün sayısı: $productCount")
                    
                    // Bildirim içeriğini detaylı logla
                    Log.d(TAG, "Bildirim içeriği: $notification")
                    
                    if (productCount == 0) {
                        Log.e(TAG, "HATA: Bildirimde ürün yok! Bildirim içeriği: $notification")
                        _errorMessage.postValue("Bildirimde ürün bulunamadı!")
                        return@launch
                    }
                    
                    // Ürünleri detaylı logla
                    for (i in 0 until productCount) {
                        val product = productsArray.getJSONObject(i)
                        Log.d(TAG, "Ürün $i: $product")
                    }
                    
                    withContext(Dispatchers.IO) {
                        // Ana bildirimi kaydet
                        val now = Date()
                        val notificationEntity = Notification(
                            id = notification.getInt("notification_id"),
                            orderNumber = notification.getString("order_number"),
                            computerId = notification.getString("computer_id"),
                            status = "pending",
                            createdAt = now,
                            updatedAt = now
                        )
                        notificationDao.insertNotification(notificationEntity)
                        Log.d(TAG, "Bildirim veritabanına kaydedildi - ID: ${notificationEntity.id}")
                        
                        // Ürünleri kaydet
                        notification.getJSONArray("products").let { products ->
                            Log.d(TAG, "Kaydedilecek ürün sayısı: ${products.length()}")
                            
                            val savedProducts = mutableListOf<NotificationProduct>()
                            
                            for (i in 0 until products.length()) {
                                val product = products.getJSONObject(i)
                                
                                // Stok ID ve ürün adı bilgilerini kontrol et
                                val stockId = if (product.has("stock_id")) product.getInt("stock_id") else null
                                val productName = if (product.has("product_name") || product.has("name")) {
                                    if (product.has("product_name")) {
                                        product.getString("product_name")
                                    } else {
                                        product.getString("name")
                                    }
                                } else null
                                
                                // Ürün verilerini logla
                                Log.d(TAG, "Ürün ekleniyor - Barkod: ${product.getString("barcode")}, Adet: ${product.getInt("quantity")}")
                                
                                val notificationProduct = NotificationProduct(
                                    notificationId = notificationEntity.id,
                                    barcode = product.getString("barcode"),
                                    quantity = product.getInt("quantity"),
                                    shelfAddress = product.getString("shelf_address"),
                                    gender = product.getString("gender"),
                                    cover = product.getString("cover"),
                                    stockId = stockId,
                                    productName = productName
                                )
                                
                                productDao.insertProduct(notificationProduct)
                                savedProducts.add(notificationProduct)
                                Log.d(TAG, "Ürün veritabanına kaydedildi - Barkod: ${notificationProduct.barcode}")
                            }
                            
                            // Kaydedilen ürün sayısını kontrol et
                            val savedProductCount = productDao.getProductsForNotification(notificationEntity.id).size
                            Log.d(TAG, "Veritabanına kaydedilen ürün sayısı: $savedProductCount")
                            
                            if (savedProductCount == 0) {
                                Log.e(TAG, "HATA: Veritabanına hiç ürün kaydedilmedi! Bildirim ID: ${notificationEntity.id}")
                            } else if (savedProductCount != products.length()) {
                                Log.e(TAG, "HATA: Veritabanına kaydedilen ürün sayısı beklenen sayıdan farklı! Beklenen: ${products.length()}, Kaydedilen: $savedProductCount")
                            }
                        }
                    }
                    loadNotifications()
                } catch (e: Exception) {
                    Log.e(TAG, "Bildirim işleme hatası", e)
                    _errorMessage.postValue("Bildirim işlenemedi: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Bildirim işlenirken hata oluştu", e)
            _errorMessage.postValue("Bildirim işlenirken hata oluştu: ${e.message}")
        }
    }

    fun loadNotifications() {
        viewModelScope.launch {
            try {
                val notifications = withContext(Dispatchers.IO) {
                    notificationDao.getAll().map { notification ->
                        val products = productDao.getProductsForNotification(notification.id)
                        
                        // Ürün sayısını logla
                        Log.d(TAG, "Bildirim yükleniyor - ID: ${notification.id}, Sipariş: ${notification.orderNumber}, Ürün sayısı: ${products.size}")
                        
                        // Ürün sayısı sıfır ise uyarı logla
                        if (products.isEmpty()) {
                            Log.e(TAG, "UYARI: Bildirimde ürün yok! ID: ${notification.id}, Sipariş: ${notification.orderNumber}")
                        }
                        
                        NotificationItem(
                            id = notification.id,
                            title = "Sipariş: ${notification.orderNumber}",
                            message = "Bilgisayar: ${notification.computerId}",
                            timestamp = notification.createdAt.time,
                            status = if (notification.status == "completed") NotificationStatus.COMPLETED else NotificationStatus.PENDING,
                            products = products
                        )
                    }
                }
                _notifications.postValue(notifications)
            } catch (e: Exception) {
                Log.e(TAG, "Bildirimler yüklenirken hata oluştu", e)
                _errorMessage.postValue("Bildirimler yüklenirken hata oluştu: ${e.message}")
            }
        }
    }

    fun completeNotification(notification: NotificationItem) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val dbNotification = notificationDao.getNotificationById(notification.id)
                    if (dbNotification != null) {
                        val now = Date().time
                        // Bildirim durumunu tamamlandı olarak güncelle
                        notificationDao.updateStatus(
                            id = notification.id,
                            status = "completed",
                            updatedAt = now,
                            completedAt = now,
                            assignedDeviceId = dbNotification.assignedDeviceId,
                            assignedDeviceName = dbNotification.assignedDeviceName
                        )
                        
                        // Bildirim durumunu sunucuya gönder
                        socketManager.sendNotificationStatus(notification.id, "completed")
                        
                        // Bildirimi ve ilişkili ürünleri yerel veritabanından sil
                        // Android cihazda tamamlanan bildirimler artık görünmeyecek
                        notificationDao.deleteNotification(notification.id)
                        productDao.deleteProductsForNotification(notification.id)
                        
                        Log.d(TAG, "Bildirim tamamlandı ve yerel veritabanından silindi - ID: ${notification.id}")
                    }
                }
                
                // Bildirimleri yeniden yükle
                loadNotifications()
                
                // Başarı mesajı göster
                _errorMessage.postValue("Bildirim tamamlandı")
            } catch (e: Exception) {
                Log.e(TAG, "Bildirim tamamlanırken hata oluştu", e)
                _errorMessage.postValue("Bildirim tamamlanırken hata oluştu: ${e.message}")
            }
        }
    }

    // SocketManager bağlantı durumunu kontrol etmek için yardımcı metod
    fun isConnected(): Boolean {
        return socketManager.isConnected()
    }

    override fun onCleared() {
        super.onCleared()
        disconnect()
    }

    companion object {
        private const val TAG = "MainViewModel"
    }
}

data class NotificationItem(
    val id: Int,
    val title: String,
    val message: String,
    val timestamp: Long,
    var status: NotificationStatus,
    val products: List<NotificationProduct> = emptyList()
)

enum class NotificationStatus {
    PENDING,
    COMPLETED
} 