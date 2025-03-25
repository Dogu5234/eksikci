package com.eksikci.android.ui.notifications

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.eksikci.android.R
import com.eksikci.android.data.Product

class ProductAdapter : ListAdapter<Product, ProductAdapter.ProductViewHolder>(DIFF_CALLBACK) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = getItem(position)
        holder.bind(product)
    }

    class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val barcodeTextView: TextView = itemView.findViewById(R.id.barcode_text_view)
        private val productNameTextView: TextView = itemView.findViewById(R.id.product_name_text_view)
        private val quantityTextView: TextView = itemView.findViewById(R.id.quantity_text_view)
        private val shelfAddressTextView: TextView = itemView.findViewById(R.id.shelf_address_text_view)
        private val genderTextView: TextView = itemView.findViewById(R.id.gender_text_view)
        private val coverTextView: TextView = itemView.findViewById(R.id.cover_text_view)

        fun bind(product: Product) {
            barcodeTextView.text = product.barcode
            
            if (product.productName.isNullOrEmpty()) {
                productNameTextView.visibility = View.GONE
            } else {
                productNameTextView.visibility = View.VISIBLE
                productNameTextView.text = product.productName
            }
            
            quantityTextView.text = itemView.context.getString(R.string.quantity_format, product.quantity)
            shelfAddressTextView.text = product.shelfAddress
            genderTextView.text = product.gender
            coverTextView.text = product.cover
        }
    }

    companion object {
        private val DIFF_CALLBACK = object : DiffUtil.ItemCallback<Product>() {
            override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
                return oldItem.id == newItem.id
            }

            override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
                return oldItem == newItem
            }
        }
    }
} 