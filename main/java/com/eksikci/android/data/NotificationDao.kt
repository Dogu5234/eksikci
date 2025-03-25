package com.eksikci.android.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface NotificationDao {
    @Query("SELECT * FROM notifications ORDER BY createdAt DESC")
    suspend fun getAll(): List<Notification>
    
    @Transaction
    @Query("SELECT * FROM notifications WHERE status IN (:statuses) ORDER BY createdAt ASC")
    fun getNotificationsByStatus(vararg statuses: String): LiveData<List<NotificationWithProducts>>
    
    @Transaction
    @Query("SELECT * FROM notifications WHERE id = :id")
    suspend fun getNotificationWithProducts(id: Int): NotificationWithProducts?
    
    @Transaction
    @Query("SELECT * FROM notifications WHERE id = :id")
    fun getNotificationWithProductsLiveData(id: Int): LiveData<NotificationWithProducts>
    
    @Query("SELECT * FROM notifications WHERE id = :id")
    suspend fun getNotificationById(id: Int): Notification?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNotification(notification: Notification)
    
    @Query("UPDATE notifications SET status = :status, updatedAt = :updatedAt, completedAt = :completedAt, assignedDeviceId = :assignedDeviceId, assignedDeviceName = :assignedDeviceName WHERE id = :id")
    suspend fun updateStatus(id: Int, status: String, updatedAt: Long, completedAt: Long?, assignedDeviceId: String? = null, assignedDeviceName: String? = null)
    
    @Query("UPDATE notifications SET status = :status, updatedAt = :updatedAt, completedAt = :completedAt WHERE id = :id")
    suspend fun updateNotificationStatus(id: Int, status: String, updatedAt: Long, completedAt: Long?)
    
    @Query("SELECT * FROM notifications WHERE status = 'completed' AND completedAt < :timestamp")
    suspend fun getOldCompletedNotifications(timestamp: Long): List<Notification>
    
    @Query("DELETE FROM notifications WHERE status = 'completed' AND completedAt < :timestamp")
    suspend fun deleteOldCompletedNotifications(timestamp: Long)
    
    @Query("DELETE FROM notifications WHERE id = :id")
    suspend fun deleteNotification(id: Int)
    
    @Query("SELECT COUNT(*) FROM notifications WHERE status IN ('pending', 'assigned')")
    fun getActiveNotificationCount(): LiveData<Int>
    
    @Query("SELECT * FROM notifications WHERE orderNumber = :orderNumber")
    suspend fun getNotificationByOrderNumber(orderNumber: String): List<Notification>
} 