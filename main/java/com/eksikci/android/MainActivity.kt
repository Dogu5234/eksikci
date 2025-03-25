package com.eksikci.android

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.eksikci.android.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.eksikci.android.ui.NotificationAdapter
import com.eksikci.android.viewmodel.MainViewModel
import android.widget.Toast
import android.os.Handler
import android.os.Looper
import com.eksikci.android.network.ConnectionState

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var notificationAdapter: NotificationAdapter
    private val PREFS_NAME = "EksikciPrefs"
    private val DEVICE_NAME_KEY = "device_name"
    private val SERVER_URL_KEY = "server_url"
    private val DEFAULT_SERVER_URL = "http://192.168.101.79:5002"
    
    // Bağlantı kontrolü için handler
    private val handler = Handler(Looper.getMainLooper())
    private val reconnectRunnable = Runnable {
        if (!viewModel.isConnected()) {
            viewModel.connectToServer(DEFAULT_SERVER_URL, 
                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(DEVICE_NAME_KEY, ""))
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupDeviceNameInput()
        // Sunucu URL giriş alanını gizle
        binding.serverUrlContainer.visibility = View.GONE
        observeViewModel()
        
        // Manuel yeniden bağlanma butonu
        binding.reconnectButton.setOnClickListener {
            Toast.makeText(this, "Sunucuya yeniden bağlanılıyor...", Toast.LENGTH_SHORT).show()
            viewModel.connectToServer(DEFAULT_SERVER_URL, 
                getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(DEVICE_NAME_KEY, ""))
        }
    }

    private fun setupRecyclerView() {
        notificationAdapter = NotificationAdapter { notification, action ->
            when (action) {
                NotificationAdapter.Action.COMPLETE -> viewModel.completeNotification(notification)
            }
        }

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = notificationAdapter
        }
    }

    private fun setupDeviceNameInput() {
        // Kaydedilmiş cihaz adını yükle
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedDeviceName = sharedPrefs.getString(DEVICE_NAME_KEY, "")
        
        // Kaydedilmiş bir cihaz adı varsa, EditText'e yerleştir
        if (!savedDeviceName.isNullOrEmpty()) {
            binding.deviceNameInput.setText(savedDeviceName)
        }
        
        // Kaydet butonuna tıklandığında
        binding.saveDeviceNameButton.setOnClickListener {
            val deviceName = binding.deviceNameInput.text.toString().trim()
            
            if (deviceName.isEmpty()) {
                Snackbar.make(binding.root, "Lütfen bir cihaz adı girin", Snackbar.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Cihaz adını kaydet
            sharedPrefs.edit().putString(DEVICE_NAME_KEY, deviceName).apply()
            
            // Cihaz adını ViewModel'e ilet
            viewModel.setDeviceName(deviceName)
            
            // Klavyeyi kapat
            hideKeyboard()
            
            Snackbar.make(binding.root, "Cihaz adı kaydedildi: $deviceName", Snackbar.LENGTH_SHORT).show()
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val currentFocusView = currentFocus ?: binding.root
        imm.hideSoftInputFromWindow(currentFocusView.windowToken, 0)
    }

    private fun observeViewModel() {
        viewModel.notifications.observe(this) { notifications ->
            notificationAdapter.submitList(notifications)
            binding.emptyView.visibility = if (notifications.isEmpty()) View.VISIBLE else View.GONE
        }

        viewModel.connectionStatus.observe(this) { isConnected ->
            binding.connectionStatus.text = if (isConnected) 
                getString(R.string.connection_status_connected)
            else 
                getString(R.string.connection_status_disconnected)
            
            binding.connectionStatus.setBackgroundColor(
                getColor(if (isConnected) android.R.color.holo_green_light else android.R.color.holo_red_light)
            )
            
            // Bağlantı durumuna göre yeniden bağlan butonunu göster/gizle
            binding.reconnectButton.visibility = if (isConnected) View.GONE else View.VISIBLE
        }
        
        // Bağlantı durumunu daha ayrıntılı izle
        viewModel.connectionState.observe(this) { state ->
            when (state) {
                ConnectionState.CONNECTED -> {
                    binding.connectionStatus.text = "Bağlandı"
                    binding.connectionStatus.setBackgroundColor(getColor(android.R.color.holo_green_light))
                    binding.reconnectButton.visibility = View.GONE
                    // Yeniden bağlanma denemesini iptal et
                    handler.removeCallbacks(reconnectRunnable)
                }
                ConnectionState.CONNECTING -> {
                    binding.connectionStatus.text = "Bağlanıyor..."
                    binding.connectionStatus.setBackgroundColor(getColor(android.R.color.holo_orange_light))
                    binding.reconnectButton.visibility = View.GONE
                }
                ConnectionState.DISCONNECTED -> {
                    binding.connectionStatus.text = "Bağlantı Kesildi"
                    binding.connectionStatus.setBackgroundColor(getColor(android.R.color.holo_red_light))
                    binding.reconnectButton.visibility = View.VISIBLE
                    
                    // 30 saniye sonra otomatik yeniden bağlanmayı dene
                    handler.postDelayed(reconnectRunnable, 30000)
                }
            }
        }

        viewModel.errorMessage.observe(this) { message ->
            message?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Kaydedilmiş cihaz adını al
        val sharedPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val deviceName = sharedPrefs.getString(DEVICE_NAME_KEY, "")
        
        // Sunucuya bağlan - sabit adres kullanıyoruz
        viewModel.connectToServer(DEFAULT_SERVER_URL, deviceName)
    }

    override fun onPause() {
        super.onPause()
        // Handler'ı temizle
        handler.removeCallbacks(reconnectRunnable)
        // Bağlantıyı kesme - bu kısmı yorum satırına alıyoruz ki arka planda da çalışabilsin
        // viewModel.disconnect()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Uygulama tamamen kapatıldığında bağlantıyı kes
        viewModel.disconnect()
    }
} 