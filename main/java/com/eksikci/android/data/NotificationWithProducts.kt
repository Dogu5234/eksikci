package com.eksikci.android.data

import androidx.room.DatabaseView
import androidx.room.Embedded
import androidx.room.Relation

@DatabaseView(
    """
    SELECT 
        n.id AS notification_id,
        n.orderNumber AS order_number,
        n.computerId AS computer_id,
        n.status,
        n.createdAt AS created_at,
        n.updatedAt AS updated_at,
        n.completedAt AS completed_at
    FROM notifications n
    """
)
data class NotificationWithProducts(
    @Embedded
    val notification: Notification,
    
    @Relation(
        parentColumn = "id",
        entityColumn = "notificationId"
    )
    val products: List<NotificationProduct>
) 