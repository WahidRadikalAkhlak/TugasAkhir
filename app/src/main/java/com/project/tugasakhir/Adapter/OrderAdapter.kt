package com.project.tugasakhir.Adapter

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Data.Order
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ItemKeranjangBinding
import com.project.tugasakhir.databinding.ItemKeranjangProdukBinding

class OrderAdapter(
    private val orders: MutableList<Order>,
    private val onItemClick: (Order) -> Unit,
    private val onDeleteClick: (Order) -> Unit,
    private val isForKeranjangPesanan: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val ITEM_TYPE_CART = 0
        const val ITEM_TYPE_ORDER_DETAIL = 1
    }

    // This function can now be accessed by both view holders
    private fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    inner class CartViewHolder(val binding: ItemKeranjangBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val selectedOrder = orders[position]
                    onItemClick(selectedOrder)
                }
            }

            binding.btnDeleteOrder.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    deleteOrder(orders[position], position)
                }
            }
        }

        fun bind(order: Order) {
            // Bind order details to the view
            binding.email.text = order.email
            binding.userName.text = order.userName  // Display buyer's username
            binding.orderNumber.text = order.orderNumber  // Order number
            binding.statusOrder.text = order.statusOrder  // Order status
            binding.tanggalOrder.text = order.orderDate  // Order date
            binding.orderTime.text = order.orderTime  // Order time

            // Load image from either imageUrl or imageBase64List
            if (order.imageUrls.isNotEmpty()) {
                Glide.with(binding.imageView.context)
                    .load(order.imageUrls[0])
                    .placeholder(R.drawable.image_icon)
                    .error(R.drawable.image_icon)
                    .into(binding.imageView)
            } else if (order.imageBase64List.isNotEmpty()) {
                val bitmap = base64ToBitmap(order.imageBase64List[0])
                if (bitmap != null) {
                    binding.imageView.setImageBitmap(bitmap)
                } else {
                    binding.imageView.setImageResource(R.drawable.image_icon)
                }
            } else {
                binding.imageView.setImageResource(R.drawable.image_icon)
            }
        }
    }

    inner class OrderDetailViewHolder(val binding: ItemKeranjangProdukBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val selectedOrder = orders[position]
                    onItemClick(selectedOrder)
                }
            }

            binding.plus.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val currentOrder = orders[position]
                    currentOrder.quantity += 1
                    updateOrderInFirestore(currentOrder)
                    notifyItemChanged(position)
                }
            }

            binding.mines.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val currentOrder = orders[position]
                    if (currentOrder.quantity > 1) {
                        currentOrder.quantity -= 1
                        updateOrderInFirestore(currentOrder)
                        notifyItemChanged(position)
                    }
                }
            }

            binding.btnDeleteOrder.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    deleteOrder(orders[position], position)
                }
            }
        }

        fun bind(order: Order) {
            binding.productName.text = order.productName
            binding.productType.text = order.productType
            binding.description.text = order.description
            binding.totalPrice.text = "Rp ${String.format("%,.0f", order.totalPrice)}"
            binding.banyakProduk.text = order.quantity.toString()

            // Load image from either imageUrl or imageBase64List
            if (order.imageUrls.isNotEmpty()) {
                // Load image using Glide from URL
                Glide.with(binding.imageView.context)
                    .load(order.imageUrls[0])  // Assuming the first image URL is the correct image
                    .placeholder(R.drawable.image_icon)  // Placeholder image while loading
                    .error(R.drawable.image_icon)  // Error image if loading fails
                    .into(binding.imageView)  // Bind image to the ImageView
            } else if (order.imageBase64List.isNotEmpty()) {
                // Convert base64 image data to Bitmap and display
                val bitmap = base64ToBitmap(order.imageBase64List[0])
                if (bitmap != null) {
                    binding.imageView.setImageBitmap(bitmap)  // Set image if valid bitmap
                } else {
                    binding.imageView.setImageResource(R.drawable.image_icon)  // Default image
                }
            } else {
                // No image available, set default image
                binding.imageView.setImageResource(R.drawable.image_icon)
            }
        }
    }

    private fun deleteOrder(order: Order, position: Int) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val orderRef = FirebaseFirestore.getInstance()
                .collection("carts")
                .document(currentUser.uid)
                .collection("items")
                .document(order.docId)
            orderRef.delete()
                .addOnSuccessListener {
                    Log.d("OrderAdapter", "Order deleted successfully")
                    orders.removeAt(position) // Remove from local list
                    notifyItemRemoved(position) // Notify adapter of item removal
                    // Update itemCount and totalPrice if necessary
                }
                .addOnFailureListener { e ->
                    Log.e("OrderAdapter", "Error deleting order: ${e.message}")
                }
        }
    }

    private fun updateOrderInFirestore(order: Order) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val orderRef = FirebaseFirestore.getInstance()
                .collection("carts")
                .document(currentUser.uid)
                .collection("items")
                .document(order.docId)

            orderRef.update("quantity", order.quantity)
                .addOnSuccessListener {
                    Log.d("OrderAdapter", "Order updated successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("OrderAdapter", "Error updating order: ${e.message}")
                }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (isForKeranjangPesanan) ITEM_TYPE_ORDER_DETAIL else ITEM_TYPE_CART
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_TYPE_CART -> {
                val binding = ItemKeranjangBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                CartViewHolder(binding)
            }
            ITEM_TYPE_ORDER_DETAIL -> {
                val binding = ItemKeranjangProdukBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                OrderDetailViewHolder(binding)
            }
            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val order = orders[position]
        when (holder) {
            is CartViewHolder -> holder.bind(order)
            is OrderDetailViewHolder -> holder.bind(order)
        }
    }

    override fun getItemCount(): Int = orders.size

    fun setOrders(newOrders: List<Order>) {
        orders.clear()
        orders.addAll(newOrders)
        notifyDataSetChanged()
    }
}