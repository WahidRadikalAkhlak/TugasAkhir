package com.project.tugasakhir.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.project.tugasakhir.Data.Product
import com.project.tugasakhir.databinding.ItemProductBinding

class KatalogAdapter(
    private val productList: MutableList<Product> = mutableListOf()
) : RecyclerView.Adapter<KatalogAdapter.KatalogViewHolder>() {

    inner class KatalogViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.tvProductName.text = product.productName
            binding.HargaBarang.text = product.pricePerUnit.toString()
            binding.tvProductType.text = product.productType  // jika ada TextView productType di layout

            Glide.with(binding.imgProduct.context)
                .load(product.imageUrl)
                .into(binding.imgProduct)
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
}
