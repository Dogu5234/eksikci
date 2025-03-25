package com.eksikci.android.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: Product)
    
    @Query("SELECT * FROM products WHERE notificationId = :notificationId")
    suspend fun getProductsByNotificationId(notificationId: Int): List<Product>
    
    @Query("DELETE FROM products WHERE notificationId = :notificationId")
    suspend fun deleteProductsByNotificationId(notificationId: Int)
} 