package com.project.tugasakhir.Cart

import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Adapter.OrderAdapter
import com.project.tugasakhir.Data.Order
import com.project.tugasakhir.Data.Product
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ActivityKeranjangPesananBinding

class KeranjangPesananActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKeranjangPesananBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val products = mutableListOf<Product>() // Add the products list

    private val orders = mutableListOf<Order>()
    private lateinit var adapter: OrderAdapter

    private var itemCount = 0
    private var totalPrice = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKeranjangPesananBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize RecyclerView and Adapter
        adapter = OrderAdapter(orders, products, { selectedOrder ->
            Toast.makeText(this, "Order dipilih: ${selectedOrder.productName}", Toast.LENGTH_SHORT)
                .show()
        }, { orderToDelete ->
            hapusPesanan(orderToDelete)
        }, isForKeranjangPesanan = true)

        binding.rvProdukKeranjang.layoutManager = LinearLayoutManager(this)
        binding.rvProdukKeranjang.adapter = adapter

        loadUserName()
        loadCartItems()
        loadProducts() // Load products as well

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
        val currentUser  = auth.currentUser  ?: run {
            Toast.makeText(this, "User  belum login", Toast.LENGTH_SHORT).show()
            return
        }

        orders.clear()  // Clear old orders before fetching new ones
        db.collection("carts")
            .whereEqualTo("userId", currentUser .uid) // Mengambil data berdasarkan pembeli
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "Tidak ada item dalam keranjang", Toast.LENGTH_SHORT)
                        .show()
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

                        imageUrls = (doc.get("imageUrls") as? List<*>)?.filterIsInstance<String>()
                            ?: emptyList()
                        imageBase64List =
                            (doc.get("imageBase64List") as? List<*>)?.filterIsInstance<String>()
                                ?: emptyList()
                    }

                    // Add the order to the list if it has a valid order number
                    if (order.orderNumber.isNotEmpty()) {
                        orders.add(order)
                        itemCount += order.quantity
                        totalPrice += order.totalPrice
                    }
                }

                // Update UI with the first order if available
                if (orders.isNotEmpty()) {
                    binding.email.text = orders.firstOrNull()?.email ?: "Email tidak tersedia"
                    binding.orderNumber.text =
                        orders.firstOrNull()?.orderNumber ?: "Order Number tidak tersedia"
                    binding.statusOrder.text =
                        orders.firstOrNull()?.statusOrder ?: "Status Order tidak tersedia"
                    binding.tanggalOrder.text =
                        orders.firstOrNull()?.orderDate ?: "Order Date tidak tersedia"
                    binding.orderTime.text =
                        orders.firstOrNull()?.orderTime ?: "Order Time tidak tersedia"
                }

                adapter.notifyDataSetChanged()
                updateBottomLayout(itemCount, totalPrice)
                updateButtonVisibility()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Gagal memuat data keranjang: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun loadProducts() {
        db.collection("products")
            .get()
            .addOnSuccessListener { documents ->
                products.clear() // Clear the previous products
                for (doc in documents) {
                    val product = doc.toObject(Product::class.java)
                    products.add(product)
                }
                adapter.notifyDataSetChanged() // Notify adapter that the product list is ready
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memuat data produk: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun onConfirmOrder() {
        val metodePembayaran = "Bayar Ditempat"
        val pesanKepadaPenjual = binding.edittextPesan.text.toString().trim()

        if (pesanKepadaPenjual.isEmpty()) {
            binding.edittextPesan.error = "Pesan tidak boleh kosong"
            return
        }

        // Update the status of each order to "Menunggu Konfirmasi Pembelian Anda" and save to Firestore
        orders.forEach { order ->
            // Update order status to "Menunggu Konfirmasi Pembelian Anda" in Firestore
            order.statusOrder = "Menunggu Konfirmasi Pembelian Anda"
            order.metodePembayaran = metodePembayaran
            order.pesanKepadaPenjual = pesanKepadaPenjual

            updateOrderStatus(
                order,
                "Menunggu Konfirmasi Pembelian Anda",
                metodePembayaran,
                pesanKepadaPenjual
            )

            // Remove the confirmed order from the local list
            orders.remove(order)
        }

        // Refresh the cart after confirmation
        loadCartItems()

        binding.confirmButton.visibility = View.GONE
        binding.cancelButton.visibility = View.VISIBLE
        updateButtonVisibility()
        updateBottomLayout(itemCount, totalPrice)
    }


    private fun onCancelOrder() {
        val metodePembayaran = "Bayar Ditempat"
        val pesanKepadaPenjual = binding.edittextPesan.text.toString().trim()

        if (pesanKepadaPenjual.isEmpty()) {
            binding.edittextPesan.error = "Pesan tidak boleh kosong"
            return
        }

        orders.forEach { order ->
            // Update the status to "Pesanan Anda Dibatalkan"
            order.statusOrder = "Pesanan Anda Dibatalkan"
            order.metodePembayaran = metodePembayaran
            order.pesanKepadaPenjual = pesanKepadaPenjual

            // Update the order in Firestore
            updateOrderStatus(
                order,
                "Pesanan Anda Dibatalkan",
                metodePembayaran,
                pesanKepadaPenjual
            )
        }

        binding.confirmButton.visibility = View.GONE
        binding.cancelButton.visibility = View.GONE
        loadCartItems() // Reload the cart items after the cancellation

        // Update UI after the status change
        adapter.notifyDataSetChanged()
        updateButtonVisibility()
        updateBottomLayout(itemCount, totalPrice)
    }

    private fun updateButtonVisibility() {
        // Check if all orders are confirmed or canceled
        val allConfirmedOrCancelled = orders.all {
            it.statusOrder == "Menunggu Konfirmasi Pembelian Anda" || it.statusOrder == "Pesanan Dibatalkan"
        }

        // If all orders are confirmed or canceled, hide the confirm button
        if (allConfirmedOrCancelled) {
            binding.confirmButton.visibility = View.GONE // Hanya sembunyikan konfirmasi pesanan
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

        if (order.orderNumber.isEmpty()) { // Ensure orderNumber is not empty
            Toast.makeText(this, "Order Number tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        // Accessing the document directly by orderNumber
        val docRef = db.collection("carts").document(order.orderNumber)

        // Update the order fields directly in Firestore
        docRef.update(
            "statusOrder", newStatus,
            "metodePembayaran", metodePembayaran,
            "pesanKepadaPenjual", pesanKepadaPenjual
        )
            .addOnSuccessListener {
                Log.d("FirestoreUpdate", "Order successfully updated with status: $newStatus")
                Toast.makeText(this, "Status order berhasil diperbarui", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreUpdate", "Error updating order: ${e.message}")
                Toast.makeText(this, "Gagal update order: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun hapusPesanan(order: Order) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        if (order.orderNumber.isEmpty()) {  // Ensure orderNumber exists
            Toast.makeText(this, "Order Number tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        // Delete the order from Firestore using orderNumber as the document ID
        val cartRef =
            db.collection("carts").document(order.orderNumber) // Use orderNumber as document ID
        cartRef.delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Pesanan telah dihapus dari Keranjang", Toast.LENGTH_SHORT)
                    .show()
                loadCartItems()  // Reload orders after deletion
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menghapus pesanan: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }
}
