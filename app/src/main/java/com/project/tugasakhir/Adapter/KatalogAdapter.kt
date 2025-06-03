package com.project.tugasakhir.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.project.tugasakhir.Data.Product
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ItemProductBinding

class KatalogAdapter(
    private val productList: MutableList<Product> = mutableListOf()
) : RecyclerView.Adapter<KatalogAdapter.KatalogViewHolder>() {

    inner class KatalogViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.tvProductName.text = product.productName
            binding.tvProductType.text = product.productType

            val price = product.pricePerUnit
            binding.HargaBarang.text = if (price != null && price > 0) {
                "Rp ${String.format("%,.0f", price)}"
            } else {
                "Harga belum tersedia"
            }

            val imageUrl = product.imageUrls.firstOrNull()
            if (!imageUrl.isNullOrBlank()) {
                Glide.with(binding.imgProduct.context)
                    .load(imageUrl)
                    .placeholder(R.drawable.image_icon)
                    .into(binding.imgProduct)
            } else {
                binding.imgProduct.setImageResource(R.drawable.image_icon)
            }
        }
    }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KatalogViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return KatalogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: KatalogViewHolder, position: Int) {
        holder.bind(productList[position])
    }

    override fun getItemCount(): Int = productList.size

    fun updateData(newList: List<Product>) {
        productList.clear()
        productList.addAll(newList)
        notifyDataSetChanged()
    }

    fun clearData() {
        productList.clear()
        notifyDataSetChanged()
    }
}
