package com.project.tugasakhir.Adapter

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.project.tugasakhir.Data.Product
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ItemProductBinding

class KatalogAdapter(
    private val productList: MutableList<Product> = mutableListOf(),
    private val onItemClick: (Product) -> Unit,
    private val isRecommendation: Boolean = false // Display likes count in recommendations
) : RecyclerView.Adapter<KatalogAdapter.KatalogViewHolder>() {

    inner class KatalogViewHolder(private val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            binding.tvProductName.text = product.productName
            binding.tvProductType.text = product.productType
            binding.HargaBarang.text = setPriceText(product.pricePerUnit)

            // Set likes count
            binding.likesCount.text = "${product.likesCount} Likes"
            binding.likesCount.visibility = View.VISIBLE

            // Set product image
            val imageUrl = product.imageUrls.firstOrNull()
            if (!imageUrl.isNullOrBlank()) {
                Glide.with(binding.imgProduct.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.image_icon)
                    .into(binding.imgProduct)
            } else {
                binding.imgProduct.setImageResource(R.drawable.image_icon)
            }

            // Handle item click to open product details
            itemView.setOnClickListener {
                onItemClick(product)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KatalogViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return KatalogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: KatalogViewHolder, position: Int) {
        val product = productList[position]
        holder.bind(product)
    }

    override fun getItemCount(): Int = productList.size

    // Update the data in the adapter
    fun updateData(newList: List<Product>) {
        productList.clear()
        productList.addAll(newList)
        notifyDataSetChanged()
    }

    // Format price for display
    private fun setPriceText(pricePerUnit: Double?): String {
        return if (pricePerUnit != null && pricePerUnit > 0) {
            "Rp ${String.format("%,.0f", pricePerUnit)}"
        } else {
            "Harga belum tersedia"
        }
    }
}
