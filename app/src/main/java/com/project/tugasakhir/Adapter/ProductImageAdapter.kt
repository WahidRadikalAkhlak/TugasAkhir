package com.project.tugasakhir.Adapter

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.project.tugasakhir.Data.Product
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ItemProductBinding

class ProductImageAdapter(
    private val products: MutableList<Product>,
    private val onItemClick: (Product) -> Unit
) : RecyclerView.Adapter<ProductImageAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.tvProductName.text = product.productName
            binding.tvProductType.text = product.productType
            binding.deskripsiProduk.text = product.description
            binding.tvStockAvailable.text = "Stock: ${product.stockAvailable}"
            binding.HargaBarang.text = if (product.pricePerUnit > 0) {
                "Rp ${String.format("%,.0f", product.pricePerUnit)}"
            } else {
                "Harga belum tersedia"
            }

            // Tampilkan gambar dari URL atau Base64, fallback ke placeholder
            when {
                product.imageUrls.isNotEmpty() -> {
                    Glide.with(binding.imgProduct.context)
                        .load(product.imageUrls[0])
                        .placeholder(R.drawable.image_icon)
                        .error(R.drawable.image_icon)
                        .into(binding.imgProduct)
                }
                product.imageBase64List.isNotEmpty() -> {
                    base64ToBitmap(product.imageBase64List[0])?.let {
                        binding.imgProduct.setImageBitmap(it)
                    } ?: binding.imgProduct.setImageResource(R.drawable.image_icon)
                }
                else -> {
                    binding.imgProduct.setImageResource(R.drawable.image_icon)
                }
            }

            binding.root.setOnClickListener { onItemClick(product) }
        }

        private fun base64ToBitmap(base64Str: String): Bitmap? {
            return try {
                val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
                android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                Log.e("ProductImageAdapter", "Failed to decode base64 image", e)
                null
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun getItemCount(): Int = products.size

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }

    fun updateData(newList: List<Product>) {
        products.clear()
        products.addAll(newList)
        notifyDataSetChanged()
    }
}
