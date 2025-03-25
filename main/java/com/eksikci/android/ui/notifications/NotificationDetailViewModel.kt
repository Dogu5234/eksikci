package com.eksikci.android.ui.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.eksikci.android.data.AppDatabase
import com.eksikci.android.data.NotificationRepository
import com.eksikci.android.data.NotificationWithProducts
import com.eksikci.android.data.SocketManager
import kotlinx.coroutines.launch
import java.util.*

class NotificationDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NotificationRepository
    private val socketManager: SocketManager

    init {
        val database = AppDatabase.getDatabase(application)
        repository = NotificationRepository(database.notificationDao(), database.productDao())
        socketManager = SocketManager.getInstance(application)
    }

    fun getNotificationWithProducts(notificationId: Int): LiveData<NotificationWithProducts> {
        return repository.getNotificationWithProductsLiveData(notificationId)
    }

    suspend fun completeNotification(notificationId: Int, deviceId: String, deviceName: String) {
        val notification = repository.getNotificationById(notificationId)
        notification?.let {
            val currentTimeMillis = System.currentTimeMillis()
            repository.updateStatus(
                notificationId, 
                "completed", 
                currentTimeMillis, 
                currentTimeMillis,
                deviceId,
                deviceName
            )
            socketManager.sendNotificationUpdate(it.orderNumber, "completed")
        }
    }
} 