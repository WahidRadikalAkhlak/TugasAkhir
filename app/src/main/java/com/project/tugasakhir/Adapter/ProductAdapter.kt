package com.project.tugasakhir.Adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.project.tugasakhir.Data.Product
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ItemProductBinding

class ProductAdapter(
    private val products: MutableList<Product>
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            binding.tvProductName.text = product.productName
            binding.tvProductType.text = product.productType
            binding.deskripsiProduk.text = product.description
            binding.tvStockAvailable.text = "Stock: ${product.stockAvailable}"
            binding.HargaBarang.text = "Rp ${String.format("%,.0f", product.pricePerUnit)}"

            if (product.imageUrls.isNotEmpty()) {
                Glide.with(binding.imgProduct.context)
                    .load(product.imageUrls[0])
                    .placeholder(R.drawable.image_icon)
                    .error(R.drawable.image_icon)
                    .into(binding.imgProduct)
            } else if (product.imageBase64List.isNotEmpty()) {
                val bitmap = base64ToBitmap(product.imageBase64List[0])
                if (bitmap != null) {
                    binding.imgProduct.setImageBitmap(bitmap)
                } else {
                    binding.imgProduct.setImageResource(R.drawable.image_icon)
                }
            } else {
                binding.imgProduct.setImageResource(R.drawable.image_icon)
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

    fun updateData(newProducts: List<Product>) {
        products.clear()
        products.addAll(newProducts)
        notifyDataSetChanged()
    }

    private fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }
}
