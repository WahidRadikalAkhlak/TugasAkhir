package com.project.tugasakhir.Adapter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
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

    inner class CartViewHolder(val binding: ItemKeranjangBinding) :
        RecyclerView.ViewHolder(binding.root) {
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
                    Log.d("OrderAdapter", "Delete button clicked for order at position: $position")
                    onDeleteClick(orders[position])  // Panggil fungsi hapus pesanan di Fragment
                }
            }
        }

        fun bind(order: Order) {
            val product = getProductForOrder(order)

            if (product != null) {
                binding.userName.text = product.userName  // Penjual name
                binding.orderNumber.text = order.orderNumber
                binding.statusOrder.text = order.statusOrder
                binding.tanggalOrder.text = order.orderDate
                binding.orderTime.text = order.orderTime
            }
        }
    }

    inner class OrderDetailViewHolder(val binding: ItemKeranjangProdukBinding) :
        RecyclerView.ViewHolder(binding.root) {
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
                    Log.d("OrderAdapter", "Delete button clicked for order at position: $position")
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

                // Check if imageBase64List is available in the order
                if (order.imageBase64List.isNotEmpty()) {
                    val base64Image = order.imageBase64List[0]
                    val bitmap = base64ToBitmap(base64Image)
                    if (bitmap != null) {
                        binding.imageView.setImageBitmap(bitmap)
                    } else {
                        binding.imageView.setImageResource(R.drawable.image_icon) // Default image
                    }
                } else {
                    binding.imageView.setImageResource(R.drawable.image_icon) // Default image if no base64 image
                }
            }
        }
    }

    private fun deleteOrder(order: Order, position: Int) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Pastikan 'orderNumber' digunakan sebagai document ID
            val orderRef = FirebaseFirestore.getInstance()
                .collection("carts")  // Koleksi utama 'carts'
                .document(order.orderNumber)  // Gunakan 'orderNumber' sebagai document ID

            orderRef.delete()
                .addOnSuccessListener {
                    Log.d("OrderAdapter", "Order deleted successfully")
                    if (position != RecyclerView.NO_POSITION && position < orders.size) {
                        // Hapus item dari list orders
                        orders.removeAt(position)
                        notifyItemRemoved(position)
                    }
                }
                .addOnFailureListener { e ->
                    // Tangani kegagalan penghapusan
                    Log.e("OrderAdapter", "Error deleting order: ${e.message}")
                }
        } else {
            Log.e("OrderAdapter", "User is not authenticated.")
        }
    }


    private fun updateOrderInFirestore(order: Order) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            // Ensure that 'orderNumber' is used as the document ID
            val orderRef = FirebaseFirestore.getInstance()
                .collection("carts")  // The parent collection
                .document(order.orderNumber)  // Use 'orderNumber' as document ID

            orderRef.update("quantity", order.quantity)
                .addOnSuccessListener {
                    Log.d("OrderAdapter", "Order updated successfully")
                }
                .addOnFailureListener { e ->
                    Log.e("OrderAdapter", "Error updating order: ${e.message}")
                }
        }
    }

    private fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e("OrderAdapter", "Failed to decode Base64 image", e)
            null
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (isForKeranjangPesanan) ITEM_TYPE_ORDER_DETAIL else ITEM_TYPE_CART
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            ITEM_TYPE_CART -> {
                val binding =
                    ItemKeranjangBinding.inflate(LayoutInflater.from(parent.context), parent, false)
                CartViewHolder(binding)
            }

            ITEM_TYPE_ORDER_DETAIL -> {
                val binding = ItemKeranjangProdukBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
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