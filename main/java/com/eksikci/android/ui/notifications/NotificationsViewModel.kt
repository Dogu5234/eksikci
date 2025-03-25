package com.eksikci.android.ui.notifications

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.eksikci.android.data.AppDatabase
import com.eksikci.android.data.NotificationRepository
import com.eksikci.android.data.NotificationWithProducts
import com.eksikci.android.data.SharedPreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NotificationRepository
    private val sharedPreferencesManager: SharedPreferencesManager

    init {
        val database = AppDatabase.getDatabase(application)
        repository = NotificationRepository(database.notificationDao(), database.productDao())
        sharedPreferencesManager = SharedPreferencesManager(application)
        
        // Periyodik olarak eski tamamlanmış bildirimleri temizle
        viewModelScope.launch(Dispatchers.IO) {
            cleanupOldNotifications()
        }
    }

    fun getActiveNotifications(): LiveData<List<NotificationWithProducts>> {
        return repository.getNotificationsWithProducts("pending", "assigned")
    }
    
    fun getActiveNotificationCount(): LiveData<Int> {
        return repository.getActiveNotificationsCount()
    }

    private suspend fun cleanupOldNotifications() {
        val twoDaysAgoInMillis = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(2)
        repository.deleteOldCompletedNotifications(twoDaysAgoInMillis)
    }
} 