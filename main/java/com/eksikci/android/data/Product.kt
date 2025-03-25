package com.eksikci.android.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "products",
    foreignKeys = [
        ForeignKey(
            entity = Notification::class,
            parentColumns = ["id"],
            childColumns = ["notificationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("notificationId")]
)
data class Product(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val notificationId: Int,
    val barcode: String,
    val quantity: Int,
    val shelfAddress: String,
    val gender: String,
    val cover: String,
    val stockId: Int? = null,
    val productName: String? = null
) 