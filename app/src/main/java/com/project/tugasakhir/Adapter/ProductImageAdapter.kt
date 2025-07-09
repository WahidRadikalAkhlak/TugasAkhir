package com.project.tugasakhir.Adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
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
            binding.tvProductName.text = product.productName ?: "Nama produk tidak tersedia"
            binding.tvProductType.text = product.productType ?: "Tipe produk tidak tersedia"
            binding.deskripsiProduk.text = product.description ?: "Deskripsi tidak tersedia"
            binding.tvStockAvailable.text = "Stock: ${product.stockAvailable}"
            binding.HargaBarang.text = setPriceText(product.pricePerUnit)

            binding.likesCount.visibility = View.GONE
            // Muat gambar produk
            loadProductImage(product)

            binding.root.setOnClickListener { onItemClick(product) }

            // Safely load images, checking for null/empty lists
            if (product.imageUrls.isNotEmpty()) {
                Glide.with(binding.imgProduct.context)
                    .load(product.imageUrls[0])
                    .placeholder(R.drawable.image_icon)
                    .error(R.drawable.image_icon)
                    .into(binding.imgProduct)
            } else if (product.imageBase64List.isNotEmpty()) {
                base64ToBitmap(product.imageBase64List[0])?.let {
                    binding.imgProduct.setImageBitmap(it)
                } ?: binding.imgProduct.setImageResource(R.drawable.image_icon)
            } else {
                binding.imgProduct.setImageResource(R.drawable.image_icon)
            }
            loadProductImage(product)
            // Handle click event for the product
            binding.root.setOnClickListener { onItemClick(product) }
        }

        private fun loadProductImage(product: Product) {
            // Cek jika imageUrls atau imageBase64List ada dan valid
            if (!product.imageUrls.isNullOrEmpty()) {
                Glide.with(binding.imgProduct.context)
                    .load(product.imageUrls[0])  // Ambil gambar pertama dari imageUrls
                    .placeholder(R.drawable.image_icon)
                    .error(R.drawable.image_icon)
                    .into(binding.imgProduct)
            } else if (!product.imageBase64List.isNullOrEmpty()) {
                base64ToBitmap(product.imageBase64List[0])?.let {
                    binding.imgProduct.setImageBitmap(it)
                }
                    ?: binding.imgProduct.setImageResource(R.drawable.image_icon)  // Default jika base64 gagal
            } else {
                binding.imgProduct.setImageResource(R.drawable.image_icon)  // Placeholder jika tidak ada gambar
            }
        }

        private fun base64ToBitmap(base64Str: String): Bitmap? {
            return try {
                val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
            } catch (e: Exception) {
                Log.e("ProductAdapter", "Failed to decode base64 image", e)
                null
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        holder.bind(products[position])
    }
    override fun getItemCount(): Int = products.size

    // Method to update data when filtering or when new data is available
    fun updateData(newList: List<Product>) {
        products.clear()
        products.addAll(newList)
        notifyDataSetChanged()
    }

    private fun setPriceText(pricePerUnit: Double?): String {
        return if (pricePerUnit != null && pricePerUnit > 0) {
            "Rp ${String.format("%,.0f", pricePerUnit)}"
        } else {
            "Harga belum tersedia"
        }
    }
}