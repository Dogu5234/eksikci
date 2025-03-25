package com.eksikci.android.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eksikci.android.data.NotificationProduct
import com.eksikci.android.databinding.ItemProductBinding

class ProductAdapter : ListAdapter<NotificationProduct, ProductAdapter.ViewHolder>(ProductDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ItemProductBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(
        private val binding: ItemProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: NotificationProduct) {
            binding.apply {
                // Ürün adı varsa göster
                if (!product.productName.isNullOrEmpty()) {
                    productName.text = product.productName
                } else {
                    productName.text = "Bilinmeyen Ürün"
                }
                productName.visibility = android.view.View.VISIBLE
                
                // Stok ID varsa göster
                if (product.stockId != null) {
                    productStockId.text = "Stok ID: ${product.stockId}"
                    productStockId.visibility = android.view.View.VISIBLE
                } else {
                    productStockId.visibility = android.view.View.GONE
                }
                
                productBarcode.text = "Barkod: ${product.barcode}"
                quantity.text = "${product.quantity} Adet"
                shelfAddress.text = "Raf: ${product.shelfAddress}"
                genderChip.text = product.gender
                coverChip.text = product.cover
            }
        }
    }

    private class ProductDiffCallback : DiffUtil.ItemCallback<NotificationProduct>() {
        override fun areItemsTheSame(oldItem: NotificationProduct, newItem: NotificationProduct): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: NotificationProduct, newItem: NotificationProduct): Boolean {
            return oldItem == newItem
        }
    }
} 