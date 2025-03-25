package com.eksikci.android.ui.notifications

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.eksikci.android.R
import com.eksikci.android.data.Notification
import com.eksikci.android.data.NotificationWithProducts
import com.eksikci.android.data.SharedPreferencesManager
import com.eksikci.android.ui.scan.AddMissingProductActivity
import com.eksikci.android.utils.NotificationStatusUtils
import kotlinx.coroutines.launch

class NotificationDetailActivity : AppCompatActivity() {

    private val viewModel: NotificationDetailViewModel by viewModels()
    private lateinit var sharedPreferencesManager: SharedPreferencesManager
    private var notificationId: Int = -1
    private lateinit var adapter: ProductAdapter
    private lateinit var statusTextView: TextView
    private lateinit var orderNumberTextView: TextView
    private lateinit var collectorNameTextView: TextView
    private lateinit var createdAtTextView: TextView
    private lateinit var assignedToTextView: TextView
    private lateinit var completeButton: Button
    private lateinit var addProductButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_detail)

        notificationId = intent.getIntExtra("notification_id", -1)
        if (notificationId == -1) {
            finish()
            return
        }

        sharedPreferencesManager = SharedPreferencesManager(this)

        statusTextView = findViewById(R.id.status_text_view)
        orderNumberTextView = findViewById(R.id.order_number_text_view)
        collectorNameTextView = findViewById(R.id.collector_name_text_view)
        createdAtTextView = findViewById(R.id.created_at_text_view)
        assignedToTextView = findViewById(R.id.assigned_to_text_view)
        completeButton = findViewById(R.id.complete_button)
        addProductButton = findViewById(R.id.add_product_button)

        val recyclerView = findViewById<RecyclerView>(R.id.products_recycler_view)
        adapter = ProductAdapter()
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        addProductButton.setOnClickListener {
            val intent = Intent(this, AddMissingProductActivity::class.java)
            intent.putExtra("notification_id", notificationId)
            startActivity(intent)
        }

        completeButton.setOnClickListener {
            lifecycleScope.launch {
                val deviceId = sharedPreferencesManager.getDeviceId()
                val deviceName = sharedPreferencesManager.getDeviceName()
                viewModel.completeNotification(notificationId, deviceId, deviceName)
                finish()
            }
        }

        viewModel.getNotificationWithProducts(notificationId).observe(this) { notificationWithProducts ->
            notificationWithProducts?.let {
                updateUI(it)
            }
        }
    }

    private fun updateUI(notificationWithProducts: NotificationWithProducts) {
        val notification = notificationWithProducts.notification
        
        orderNumberTextView.text = notification.orderNumber
        statusTextView.text = NotificationStatusUtils.getStatusText(this, notification.status)
        statusTextView.setTextColor(NotificationStatusUtils.getStatusColor(this, notification.status))
        
        val collectorName = notification.collectorName ?: notification.collectorNameFromDb
        collectorNameTextView.text = collectorName ?: getString(R.string.unknown)
        
        createdAtTextView.text = notification.getFormattedCreatedAt()
        
        // Show assigned device info if available
        if (!notification.assignedDeviceId.isNullOrEmpty() && !notification.assignedDeviceName.isNullOrEmpty()) {
            assignedToTextView.visibility = View.VISIBLE
            assignedToTextView.text = getString(R.string.assigned_to_device, notification.assignedDeviceName)
        } else {
            assignedToTextView.visibility = View.GONE
        }
        
        adapter.submitList(notificationWithProducts.products)
        
        // Update button visibility based on status
        if (notification.status == "completed") {
            completeButton.visibility = View.GONE
            addProductButton.visibility = View.GONE
        } else {
            completeButton.visibility = View.VISIBLE
            addProductButton.visibility = View.VISIBLE
        }
    }
} 