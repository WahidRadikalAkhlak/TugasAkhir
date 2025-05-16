package com.project.tugasakhir.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.project.tugasakhir.Data.Product
import com.project.tugasakhir.databinding.ItemProductBinding

class ProductAdapter(
    private val products: MutableList<Product>
) : RecyclerView.Adapter<ProductAdapter.ProductViewHolder>() {

    inner class ProductViewHolder(private val binding: ItemProductBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(product: Product) {
            binding.tvProductName.text = product.productName
            binding.tvProductType.text = product.productType
            binding.deskripsiProduk.text = product.description
            binding.tvStockAvailable.text = "Stock: ${product.stockAvailable}"
            binding.HargaBarang.text = product.pricePerUnit.toString()

            // Load gambar dengan Glide (pastikan product.imageUrl berisi URL string gambar)
            Glide.with(binding.imgProduct.context)
                .load(product.imageUrl)
                .placeholder(android.R.color.darker_gray)
                .centerInside()
                .into(binding.imgProduct)
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

    fun updateData(newProducts: List<Product>) {
        products.clear()
        products.addAll(newProducts)
        notifyDataSetChanged()
    }
}