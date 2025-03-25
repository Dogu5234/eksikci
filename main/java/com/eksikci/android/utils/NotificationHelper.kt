package com.eksikci.android.utils

import android.content.Context
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.eksikci.android.R

/**
 * Bildirim sesi ve titreşim işlemlerini yöneten yardımcı sınıf
 */
class NotificationHelper(private val context: Context) {
    
    private var mediaPlayer: MediaPlayer? = null
    
    /**
     * Bildirim sesi çalar ve cihazı titretir
     */
    fun playNotificationSound() {
        try {
            // Önceki çalan sesi durdur
            stopSound()
            
            // Bildirim sesini çal
            mediaPlayer = MediaPlayer.create(context, R.raw.notification_sound)
            mediaPlayer?.setOnCompletionListener { mp ->
                mp.release()
            }
            mediaPlayer?.start()
            
            // Titreşim ekle
            vibrate()
            
            Log.d(TAG, "Bildirim sesi çalındı")
        } catch (e: Exception) {
            Log.e(TAG, "Bildirim sesi çalınırken hata oluştu", e)
        }
    }
    
    /**
     * Varsayılan bildirim sesini çalar
     */
    fun playDefaultNotificationSound() {
        try {
            // Önceki çalan sesi durdur
            stopSound()
            
            // Varsayılan bildirim sesini al
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, notification)
            ringtone.play()
            
            // Titreşim ekle
            vibrate()
            
            Log.d(TAG, "Varsayılan bildirim sesi çalındı")
        } catch (e: Exception) {
            Log.e(TAG, "Varsayılan bildirim sesi çalınırken hata oluştu", e)
        }
    }
    
    /**
     * Cihazı titretir
     */
    private fun vibrate() {
        try {
            // Titreşim süresi (milisaniye)
            val vibrationDuration = 500L
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                val vibrator = vibratorManager.defaultVibrator
                vibrator.vibrate(VibrationEffect.createOneShot(vibrationDuration, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(vibrationDuration, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(vibrationDuration)
                }
            }
            
            Log.d(TAG, "Cihaz titretildi")
        } catch (e: Exception) {
            Log.e(TAG, "Titreşim sırasında hata oluştu", e)
        }
    }
    
    /**
     * Çalan sesi durdurur
     */
    fun stopSound() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }
    
    companion object {
        private const val TAG = "NotificationHelper"
    }
} 