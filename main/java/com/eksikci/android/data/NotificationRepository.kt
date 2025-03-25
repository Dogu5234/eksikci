package com.eksikci.android.data

import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NotificationRepository(
    private val notificationDao: NotificationDao,
    private val productDao: ProductDao
) {

    suspend fun getNotificationById(id: Int): Notification? {
        return withContext(Dispatchers.IO) {
            notificationDao.getNotificationById(id)
        }
    }

    suspend fun getNotificationWithProducts(id: Int): NotificationWithProducts? {
        return withContext(Dispatchers.IO) {
            notificationDao.getNotificationWithProducts(id)
        }
    }

    fun getNotificationWithProductsLiveData(id: Int): LiveData<NotificationWithProducts> {
        return notificationDao.getNotificationWithProductsLiveData(id)
    }

    fun getNotificationsWithProducts(vararg statuses: String): LiveData<List<NotificationWithProducts>> {
        return notificationDao.getNotificationsByStatus(*statuses)
    }

    fun getActiveNotificationsCount(): LiveData<Int> {
        return notificationDao.getActiveNotificationCount()
    }

    suspend fun insertNotification(notification: Notification): Long {
        return withContext(Dispatchers.IO) {
            notificationDao.insertNotification(notification)
            notification.id.toLong()
        }
    }

    suspend fun updateStatus(id: Int, status: String, updatedAt: Long, completedAt: Long?, assignedDeviceId: String? = null, assignedDeviceName: String? = null) {
        withContext(Dispatchers.IO) {
            notificationDao.updateStatus(id, status, updatedAt, completedAt, assignedDeviceId, assignedDeviceName)
        }
    }

    suspend fun deleteNotification(id: Int) {
        withContext(Dispatchers.IO) {
            productDao.deleteProductsByNotificationId(id)
            notificationDao.deleteNotification(id)
        }
    }

    suspend fun deleteOldCompletedNotifications(timestamp: Long) {
        withContext(Dispatchers.IO) {
            val notificationsToDelete = notificationDao.getOldCompletedNotifications(timestamp)
            notificationsToDelete.forEach { notification ->
                productDao.deleteProductsByNotificationId(notification.id)
            }
            notificationDao.deleteOldCompletedNotifications(timestamp)
        }
    }

    suspend fun insertProduct(product: Product) {
        withContext(Dispatchers.IO) {
            productDao.insertProduct(product)
        }
    }
} 