package com.project.tugasakhir.Adapter

import android.content.Context  // Pastikan ini yang diimport
import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.project.tugasakhir.databinding.ItemGambarProdukBinding

class ImageAdapter(
    private val imageList: List<Uri>,
    private val context: Context
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    inner class ImageViewHolder(private val binding: ItemGambarProdukBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(uri: Uri) {
            Glide.with(binding.root.context)  // bisa juga pake context jika ingin
                .load(uri)  // pakai uri yang benar
                .into(binding.itemImage)  // sesuaikan dengan id ImageView di item layout Anda
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemGambarProdukBinding.inflate(LayoutInflater.from(context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(imageList[position])
    }

    override fun getItemCount(): Int = imageList.size
}
