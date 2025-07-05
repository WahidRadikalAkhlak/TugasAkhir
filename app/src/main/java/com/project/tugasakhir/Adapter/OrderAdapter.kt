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
import com.project.tugasakhir.Data.Product
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ItemKeranjangBinding
import com.project.tugasakhir.databinding.ItemKeranjangProdukBinding

class OrderAdapter(
    private val orders: MutableList<Order>,
    private val products: List<Product>,  // List of products to match orders with
    private val onItemClick: (Order) -> Unit,
    private val onDeleteClick: (Order) -> Unit,
    private val isForKeranjangPesanan: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        const val ITEM_TYPE_CART = 0
        const val ITEM_TYPE_ORDER_DETAIL = 1
    }

    private fun getProductForOrder(order: Order): Product? {
        return products.find { it.productName == order.productName }
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
            val product = getProductForOrder(order)

            if (product != null) {
                // Bind user name (penjual) from the product
                binding.userName.text = product.userName  // Penjual name

                // Bind other order details
                binding.orderNumber.text = order.orderNumber
                binding.statusOrder.text = order.statusOrder
                binding.tanggalOrder.text = order.orderDate
                binding.orderTime.text = order.orderTime

                // Load product image from the product object (if available)
                if (product.imageUrls.isNotEmpty()) {
                    Glide.with(binding.imageView.context)
                        .load(product.imageUrls[0])  // Load the first image from the product
                        .placeholder(R.drawable.image_icon)
                        .error(R.drawable.image_icon)
                        .into(binding.imageView)
                } else {
                    binding.imageView.setImageResource(R.drawable.image_icon)  // Default image if none
                }
            }
        }
    }

    // OrderDetailViewHolder for displaying detailed order and product info
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
            val product = getProductForOrder(order)

            if (product != null) {
                binding.productName.text = order.productName
                binding.productType.text = order.productType
                binding.description.text = order.description
                binding.totalPrice.text = "Rp ${String.format("%,.0f", order.totalPrice)}"
                binding.banyakProduk.text = order.quantity.toString()

                // Load product image from the corresponding Product object
                if (product.imageUrls.isNotEmpty()) {
                    Glide.with(binding.imageView.context)
                        .load(product.imageUrls[0])  // Display product image
                        .placeholder(R.drawable.image_icon)
                        .error(R.drawable.image_icon)
                        .into(binding.imageView)
                } else {
                    binding.imageView.setImageResource(R.drawable.image_icon)  // Default image if no product image
                }
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
            is CartViewHolder -> holder.bind(order)  // Pass order to the view holder
            is OrderDetailViewHolder -> holder.bind(order)  // Same for order details
        }
    }

    override fun getItemCount(): Int = orders.size

    fun setOrders(newOrders: List<Order>) {
        orders.clear()
        orders.addAll(newOrders)
        notifyDataSetChanged()
    }
}
