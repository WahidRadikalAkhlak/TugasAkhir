package com.project.tugasakhir.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.project.tugasakhir.Data.Order
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ItemKeranjangBinding

class OrderAdapter(
    private val orders: List<Order>,
    private val onItemClick: (Order) -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(val binding: ItemKeranjangBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onItemClick(orders[position])
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemKeranjangBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]
        with(holder.binding) {
            userName.text = order.userName
            userAddress.text = order.userAddress
            nomorOrder.text = order.orderNumber.ifEmpty { "-" }
            statusOrder.text = order.status.ifEmpty { "-" }
            tanggalOrder.text = order.orderDate.ifEmpty { "-" }
            orderTime.text = order.orderTime.ifEmpty { "-" }

            val context = root.context

            // Tampilkan gambar produk dengan Glide menggunakan binding.imageView langsung
            if (order.imageUrl.isNotEmpty()) {
                Glide.with(context)
                    .load(order.imageUrl)
                    .placeholder(R.drawable.image_icon)
                    .error(R.drawable.image_icon)
                    .into(imageView)  // langsung akses binding.imageView
            } else {
                imageView.setImageResource(R.drawable.image_icon)
            }

            when (order.status) {
                "Selesai" -> statusOrder.setTextColor(context.getColor(android.R.color.holo_green_dark))
                "Memesan" -> statusOrder.setTextColor(context.getColor(android.R.color.holo_orange_dark))
                else -> statusOrder.setTextColor(context.getColor(android.R.color.black))
            }
        }
    }


    override fun getItemCount(): Int = orders.size
}
