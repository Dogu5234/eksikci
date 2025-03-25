package com.eksikci.android.data

import androidx.room.*

@Dao
interface NotificationProductDao {
    @Query("SELECT * FROM notification_products WHERE notificationId = :notificationId")
    suspend fun getProductsForNotification(notificationId: Int): List<NotificationProduct>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: NotificationProduct)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProducts(products: List<NotificationProduct>)
    
    @Delete
    suspend fun deleteProduct(product: NotificationProduct)
    
    @Query("DELETE FROM notification_products WHERE notificationId = :notificationId")
    suspend fun deleteProductsForNotification(notificationId: Int)
} 