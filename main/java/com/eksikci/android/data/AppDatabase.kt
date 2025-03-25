package com.eksikci.android.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Notification::class, NotificationProduct::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun notificationDao(): NotificationDao
    abstract fun notificationProductDao(): NotificationProductDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "notifications_database"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
        
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns to notifications table
                database.execSQL("ALTER TABLE notifications ADD COLUMN assignedDeviceId TEXT")
                database.execSQL("ALTER TABLE notifications ADD COLUMN assignedDeviceName TEXT")
                
                // Add new columns for collector info
                database.execSQL("ALTER TABLE notifications ADD COLUMN collectorName TEXT")
                database.execSQL("ALTER TABLE notifications ADD COLUMN collectorNameFromDb TEXT")
                database.execSQL("ALTER TABLE notifications ADD COLUMN notes TEXT")
            }
        }
        
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create notification_products table if it doesn't exist
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS notification_products (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        notificationId INTEGER NOT NULL,
                        barcode TEXT NOT NULL,
                        quantity INTEGER NOT NULL,
                        shelfAddress TEXT NOT NULL,
                        gender TEXT NOT NULL,
                        cover TEXT NOT NULL,
                        stockId INTEGER,
                        productName TEXT,
                        FOREIGN KEY (notificationId) REFERENCES notifications(id) ON DELETE CASCADE
                    )
                    """
                )
                
                // Create index for notification_products.notificationId
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_notification_products_notificationId ON notification_products(notificationId)"
                )
            }
        }
    }
} 