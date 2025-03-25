package com.eksikci.android.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date
import java.text.SimpleDateFormat
import java.util.*

@Entity(tableName = "notifications")
data class Notification(
    @PrimaryKey
    val id: Int,
    val orderNumber: String,
    val collectorName: String? = null,
    val collectorNameFromDb: String? = null,
    val notes: String? = null,
    val status: String,
    val createdAt: Long,
    val updatedAt: Long,
    val completedAt: Long? = null,
    val assignedDeviceId: String? = null,
    val assignedDeviceName: String? = null
) {
    fun getFormattedCreatedAt(): String {
        return formatDate(createdAt)
    }

    fun getFormattedUpdatedAt(): String {
        return formatDate(updatedAt)
    }

    fun getFormattedCompletedAt(): String? {
        return completedAt?.let { formatDate(it) }
    }

    private fun formatDate(timestamp: Long): String {
        val formatter = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault())
        return formatter.format(Date(timestamp))
    }
}

@Entity(
    tableName = "notification_products",
    foreignKeys = [
        ForeignKey(
            entity = Notification::class,
            parentColumns = ["id"],
            childColumns = ["notificationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["notificationId"])
    ]
)
data class NotificationProduct(
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