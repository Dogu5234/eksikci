package com.eksikci.android

import android.app.Application
import com.eksikci.android.data.AppDatabase

class EksikciApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Veritabanını başlat
        AppDatabase.getDatabase(this)
    }
} 