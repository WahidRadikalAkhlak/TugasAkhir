package com.project.tugasakhir.Cart

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Adapter.OrderAdapter
import com.project.tugasakhir.Data.Order
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ActivityKeranjangPesananBinding

class KeranjangPesananActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKeranjangPesananBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val orders = mutableListOf<Order>()
    private lateinit var adapter: OrderAdapter

    private var itemCount = 0
    private var totalPrice = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKeranjangPesananBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize RecyclerView and Adapter
        adapter = OrderAdapter(orders, { selectedOrder ->
            Toast.makeText(this, "Order dipilih: ${selectedOrder.productName}", Toast.LENGTH_SHORT)
                .show()
        }, { orderToDelete ->
            hapusPesanan(orderToDelete)
        }, isForKeranjangPesanan = true)

        binding.rvProdukKeranjang.layoutManager = LinearLayoutManager(this)
        binding.rvProdukKeranjang.adapter = adapter

        loadUserName()
        loadCartItems()

        // Buttons configuration
        binding.confirmButton.backgroundTintList =
            ColorStateList.valueOf(resources.getColor(R.color.btn_color, null))
        binding.cancelButton.backgroundTintList =
            ColorStateList.valueOf(resources.getColor(R.color.btn_color, null))

        binding.confirmButton.setOnClickListener {
            if (itemCount <= 0) {
                Toast.makeText(this, "Tidak ada item untuk diproses", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Confirming order", Toast.LENGTH_SHORT).show()
                onConfirmOrder()
            }
        }

        binding.cancelButton.setOnClickListener {
            Toast.makeText(this, "Cancelling order", Toast.LENGTH_SHORT).show()
            onCancelOrder()
        }
    }

    private fun loadUserName() {
        val currentUser = auth.currentUser
        binding.namaPengguna.text =
            currentUser?.displayName ?: currentUser?.email ?: "Pengguna belum login"
    }

    private fun loadCartItems() {
        val currentUser = auth.currentUser ?: run {
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show()
            return
        }
        orders.clear() // Clear the existing orders

        // Fetch items from the 'carts' collection for the current user
        db.collection("carts")
            .document(currentUser.uid) // User-specific cart
            .collection("items") // The items collection within the cart
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Tidak ada item dalam keranjang", Toast.LENGTH_SHORT).show()
                    adapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                itemCount = 0
                totalPrice = 0.0

                // Loop through all documents to retrieve order details
                for (doc in documents) {
                    val order = doc.toObject(Order::class.java).apply {
                        docId = doc.id
                        email = doc.getString("email") ?: "Email tidak tersedia"
                        userName = doc.getString("userName") ?: "Nama tidak tersedia"
                        orderNumber = doc.getString("orderNumber") ?: ""
                        orderDate = doc.getString("orderDate") ?: ""
                        orderTime = doc.getString("orderTime") ?: ""
                        statusOrder = doc.getString("statusOrder") ?: "Memesan"
                        pricePerUnit = doc.getDouble("pricePerUnit") ?: 0.0
                        productName = doc.getString("productName") ?: ""
                        productType = doc.getString("productType") ?: ""
                        quantity = doc.getLong("quantity")?.toInt() ?: 0
                        totalPrice = doc.getDouble("totalPrice") ?: 0.0
                        timestamp = doc.getTimestamp("timestamp")
                    }

                    // Only add items that are not yet confirmed or canceled
                    if (order.productName.isNotEmpty() && order.productType.isNotEmpty() &&
                        order.statusOrder != "Menunggu Konfirmasi Pembelian Anda" && order.statusOrder != "Pesanan Dibatalkan") {
                        orders.add(order)
                        itemCount += order.quantity
                        totalPrice += order.totalPrice
                    }
                }

                // Display the data for the first order in the text fields
                binding.email.text = orders.firstOrNull()?.email ?: "Email tidak tersedia"
                binding.orderNumber.text = orders.firstOrNull()?.orderNumber ?: "Order Number tidak tersedia"
                binding.statusOrder.text = orders.firstOrNull()?.statusOrder ?: "Status Order tidak tersedia"
                binding.tanggalOrder.text = orders.firstOrNull()?.orderDate ?: "Order Date tidak tersedia"
                binding.orderTime.text = orders.firstOrNull()?.orderTime ?: "Order Time tidak tersedia"

                // Notify the adapter that the data has changed
                adapter.notifyDataSetChanged()
                updateBottomLayout(itemCount, totalPrice)

                // Check and update button visibility after loading the items
                updateButtonVisibility()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memuat data keranjang: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun onConfirmOrder() {
        val metodePembayaran = "Bayar Ditempat"
        val pesanKepadaPenjual = binding.edittextPesan.text.toString()

        orders.forEach { order ->
            updateOrderStatus(
                order,
                "Menunggu Konfirmasi Pembelian Anda",
                metodePembayaran,
                pesanKepadaPenjual
            )
        }

        // Hide the confirm button once the order is confirmed
        binding.confirmButton.visibility = View.GONE
        binding.cancelButton.visibility = View.VISIBLE

        // Check and update button visibility after confirming the order
        updateButtonVisibility()
    }

    private fun onCancelOrder() {
        val metodePembayaran = "Bayar Ditempat"
        val pesanKepadaPenjual = binding.edittextPesan.text.toString()

        orders.forEach { order ->
            updateOrderStatus(order, "Pesanan Dibatalkan", metodePembayaran, pesanKepadaPenjual)
        }

        // Hide the cancel button after cancellation
        binding.confirmButton.visibility = View.GONE
        binding.cancelButton.visibility = View.VISIBLE

        // Check and update button visibility after canceling the order
        updateButtonVisibility()
    }

    private fun updateButtonVisibility() {
        // Check if all orders are confirmed or canceled
        val allConfirmedOrCancelled = orders.all {
            it.statusOrder == "Menunggu Konfirmasi Pembelian Anda" || it.statusOrder == "Pesanan Dibatalkan"
        }

        // If all orders are confirmed or canceled, hide both buttons
        if (allConfirmedOrCancelled) {
            binding.confirmButton.visibility = View.GONE
            binding.cancelButton.visibility = View.GONE
        } else {
            // Otherwise, show the confirm and cancel buttons
            binding.confirmButton.visibility = View.VISIBLE
            binding.cancelButton.visibility = View.VISIBLE
        }
    }


    private fun updateBottomLayout(itemCount: Int, totalPrice: Double) {
        binding.itemCount.text = "Item: $itemCount"
        binding.totalPrice.text = "Rp ${String.format("%,.0f", totalPrice)}"
    }

    private fun updateOrderStatus(
        order: Order,
        newStatus: String,
        metodePembayaran: String,
        pesanKepadaPenjual: String
    ) {
        val currentUser = auth.currentUser ?: return
        if (order.docId.isEmpty()) {
            Toast.makeText(this, "Order ID tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        // Reference to the document in Firestore
        val docRef = db.collection("carts")
            .document(currentUser.uid)
            .collection("items")
            .document(order.docId)

        // Update the order status, payment method, and seller message
        docRef.update(
            "statusOrder", newStatus,
            "metodePembayaran", metodePembayaran,
            "pesanKepadaPenjual", pesanKepadaPenjual
        ).addOnSuccessListener {
            // After successfully updating the status, reload the cart items
            loadCartItems()
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal update order", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hapusPesanan(order: Order) {
        val currentUser  = FirebaseAuth.getInstance().currentUser  ?: run {
            Toast.makeText(this, "User  belum login", Toast.LENGTH_SHORT).show()
            return
        }

        if (order.docId.isEmpty()) {
            Toast.makeText(this, "ID pesanan tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        val docRef = db.collection("carts")
            .document(currentUser .uid)
            .collection("items")
            .document(order.docId)

        docRef.delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Pesanan berhasil dihapus", Toast.LENGTH_SHORT).show()

                // Remove the order from the local list
                orders.remove(order)
                itemCount -= order.quantity
                totalPrice -= order.totalPrice
                adapter.notifyDataSetChanged()

                // Update bottom layout
                updateBottomLayout(itemCount, totalPrice)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menghapus pesanan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}
