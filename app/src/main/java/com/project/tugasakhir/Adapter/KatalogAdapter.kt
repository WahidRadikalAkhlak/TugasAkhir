package com.project.tugasakhir.Adapter

import android.graphics.Bitmap
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

    inner class KatalogViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            binding.tvProductName.text = product.productName ?: "Nama produk tidak tersedia"
            binding.tvProductType.text = product.productType ?: "Tipe produk tidak tersedia"
            binding.deskripsiProduk.text = product.description ?: "Deskripsi tidak tersedia"
            binding.tvStockAvailable.text = "Stock: ${product.stockAvailable}"
            binding.HargaBarang.text = setPriceText(product.pricePerUnit)
            binding.ratingBar.rating = product.avgRating
            if (isRecommendation) {
                binding.likesCount.text = "${product.likesCount} Likes"
                binding.likesCount.visibility = View.VISIBLE
            } else {
                binding.likesCount.visibility = View.GONE
            }

            if (product.discount > 0) {
                binding.discountLabel.text = "Disc ${product.discount}%"
                binding.discountLabel.visibility = View.VISIBLE
            } else {
                binding.discountLabel.visibility = View.GONE
            }
            // Muat gambar produk
            loadProductImage(product)

            // Tangani klik item untuk membuka detail produk
            binding.root.setOnClickListener { onItemClick(product) }
        }

        private fun loadProductImage(product: Product) {
            if (!product.imageUrls.isNullOrEmpty()) {
                Glide.with(binding.imgProduct.context)
                    .load(product.imageUrls[0])
                    .placeholder(R.drawable.image_icon)
                    .error(R.drawable.image_icon)
                    .into(binding.imgProduct)
            } else if (!product.imageBase64List.isNullOrEmpty()) {
                base64ToBitmap(product.imageBase64List[0])?.let {
                    binding.imgProduct.setImageBitmap(it)
                } ?: binding.imgProduct.setImageResource(R.drawable.image_icon)
            } else {
                binding.imgProduct.setImageResource(R.drawable.image_icon) // Placeholder
            }
        }

        private fun base64ToBitmap(base64Str: String): Bitmap? {
            return try {
                val decodedBytes =
                    android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                android.util.Log.e("KatalogAdapter", "Failed to decode base64 image", e)
                null
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

    // Method to update data when filtering or when new data is available
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
    fun getData(): List<Product> {
        return productList
    }
}
