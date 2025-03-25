package com.eksikci.android.ui

import android.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eksikci.android.R
import com.eksikci.android.data.NotificationProduct
import com.eksikci.android.viewmodel.NotificationItem
import com.eksikci.android.viewmodel.NotificationStatus
import com.eksikci.android.databinding.ItemNotificationBinding
import com.google.android.material.chip.Chip

class NotificationAdapter(
    private val onActionClick: (NotificationItem, Action) -> Unit
) : ListAdapter<NotificationItem, NotificationAdapter.ViewHolder>(NotificationDiffCallback()) {

    enum class Action { COMPLETE }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemNotificationBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ViewHolder(
        private val binding: ItemNotificationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val productAdapter = ProductAdapter()

        init {
            binding.productsRecyclerView.apply {
                layoutManager = LinearLayoutManager(context)
                adapter = productAdapter
            }
        }

        fun bind(notification: NotificationItem) {
            binding.apply {
                orderNumber.text = notification.title
                computerId.text = notification.message

                // Ürün sayısını göster
                val productSize = notification.products.size
                
                // Ürün sayısı 0 ise uyarı renginde ve farklı metinle göster
                if (productSize == 0) {
                    productCount.text = "⚠️ UYARI: Ürün Bulunamadı! ⚠️"
                    productCount.setTextColor(ContextCompat.getColor(itemView.context, R.color.notification_pending))
                    productCount.textSize = 16f  // Daha büyük font
                    productCount.setPadding(16, 16, 16, 16)  // Daha fazla padding
                } else {
                    productCount.text = "Toplam ${productSize} Ürün"
                    productCount.setTextColor(ContextCompat.getColor(itemView.context, R.color.purple_500))
                    productCount.textSize = 14f  // Normal font
                    productCount.setPadding(8, 8, 8, 8)  // Normal padding
                }

                // Durum chip'i
                statusChip.apply {
                    if (notification.status == NotificationStatus.COMPLETED) {
                        text = "Tamamlandı"
                        setChipBackgroundColorResource(R.color.chip_completed)
                    } else {
                        text = "Bekliyor"
                        setChipBackgroundColorResource(R.color.chip_pending)
                    }
                }

                // Tamamla butonu - sadece bekleyen bildirimlerde göster
                actionButton.apply {
                    if (notification.status == NotificationStatus.COMPLETED) {
                        visibility = View.GONE
                    } else {
                        visibility = View.VISIBLE
                        text = context.getString(R.string.button_confirm_complete)
                        setBackgroundColor(ContextCompat.getColor(context, R.color.notification_pending))
                        setOnClickListener { 
                            // Tamamlama onay diyaloğunu göster
                            showCompletionConfirmationDialog(notification)
                        }
                    }
                }
                
                // Gönder butonunu gizle (artık kullanılmıyor)
                sendButton.visibility = View.GONE

                productAdapter.submitList(notification.products)
            }
        }
        
        // Tamamlama onay diyaloğunu göster
        private fun showCompletionConfirmationDialog(notification: NotificationItem) {
            val context = itemView.context
            AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.dialog_title_confirm_completion))
                .setMessage(context.getString(R.string.dialog_message_confirm_completion))
                .setPositiveButton(context.getString(R.string.dialog_positive_button)) { _, _ ->
                    // Onaylandığında tamamlama işlemini gerçekleştir
                    onActionClick(notification, Action.COMPLETE)
                }
                .setNegativeButton(context.getString(R.string.dialog_negative_button), null)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show()
        }
    }

    private class NotificationDiffCallback : DiffUtil.ItemCallback<NotificationItem>() {
        override fun areItemsTheSame(oldItem: NotificationItem, newItem: NotificationItem) = 
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: NotificationItem, newItem: NotificationItem) = 
            oldItem == newItem
    }
} 