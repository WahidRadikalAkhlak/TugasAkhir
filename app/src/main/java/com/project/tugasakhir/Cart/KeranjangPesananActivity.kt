package com.project.tugasakhir.Cart

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Adapter.OrderAdapter
import com.project.tugasakhir.Data.Order
import com.project.tugasakhir.Data.Product
import com.project.tugasakhir.R
import com.project.tugasakhir.Ulasan.UlasanActivity
import com.project.tugasakhir.databinding.ActivityKeranjangPesananBinding

class KeranjangPesananActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKeranjangPesananBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val products = mutableListOf<Product>() // Add the products list

    private val orders = mutableListOf<Order>()
    private lateinit var adapter: OrderAdapter
    private lateinit var selectedOrderNumber: String

    private var itemCount = 0
    private var totalPrice = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKeranjangPesananBinding.inflate(layoutInflater)
        setContentView(binding.root)

        selectedOrderNumber = intent.getStringExtra("ORDER_NUMBER") ?: ""

        // Pastikan orderNumber tidak kosong
        if (selectedOrderNumber.isEmpty()) {
            Toast.makeText(this, "Order Number tidak valid", Toast.LENGTH_SHORT).show()
            finish() // Tutup activity jika orderNumber tidak valid
            return
        }

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
        loadCartItems() // Load cart items based on the selected order number
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
                // Show progress bar
                binding.progressBar.visibility = View.VISIBLE
                Toast.makeText(this, "Confirming order", Toast.LENGTH_SHORT).show()

                // Confirm the order and navigate to review
                onConfirmOrder()

                // Hide progress bar after process
                binding.progressBar.visibility = View.GONE
            }
        }

        binding.cancelButton.setOnClickListener {
            // Menampilkan ProgressBar
            binding.progressBar.visibility = View.VISIBLE

            Toast.makeText(this, "Cancelling order", Toast.LENGTH_SHORT).show()
            onCancelOrder()

            // Sembunyikan ProgressBar setelah proses selesai
            binding.progressBar.visibility = View.GONE
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

        orders.clear()  // Clear old orders before fetching new ones
        db.collection("carts")
            .whereEqualTo("userId", currentUser.uid)  // Mengambil data berdasarkan pembeli
            .whereEqualTo("orderNumber", selectedOrderNumber) // Pastikan orderNumber yang digunakan konsisten
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
                        alamatToko = doc.getString("alamatToko") ?: ""  // Set alamatToko
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
                    binding.alamat.text = orders.firstOrNull()?.alamatToko ?: "Alamat toko tidak tersedia"
                }
                loadPesanKepadaPenjual()
                adapter.notifyDataSetChanged()
                updateBottomLayout(itemCount, totalPrice)
                updateButtonVisibility()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memuat data keranjang: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun loadPesanKepadaPenjual() {
        // Get the message from Firestore
        val order = orders.firstOrNull() ?: return
        val orderNumber = order.orderNumber

        db.collection("carts")
            .document(orderNumber)
            .get()
            .addOnSuccessListener { document ->
                val message = document.getString("pesanKepadaPenjual")
                if (message != null) {
                    binding.edittextPesan.setText(message)
                }
            }
            .addOnFailureListener { e ->
                Log.e("KeranjangPesanan", "Failed to load message: ${e.message}")
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
        var pesanKepadaPenjual = binding.edittextPesan.text.toString().trim()

        // Check if the order status is "Memesan" (can edit message)
        orders.forEach { order ->
            if (order.statusOrder == "Memesan") {
                if (pesanKepadaPenjual.isEmpty()) {
                    binding.edittextPesan.error = "Pesan tidak boleh kosong"
                    return@forEach
                }

                // Update order status to "Menunggu Konfirmasi Penjual" and save the message
                order.statusOrder = "Menunggu Konfirmasi Penjual"
                order.metodePembayaran = metodePembayaran
                order.pesanKepadaPenjual = pesanKepadaPenjual
                updateOrderStatus(
                    order,
                    "Menunggu Konfirmasi Penjual", // First confirmation status
                    metodePembayaran,
                    pesanKepadaPenjual
                )
                binding.edittextPesan.isEnabled = false
            } else if (order.statusOrder == "Pesanan Sedang Dikemas") {
                // When order status is "Pesanan Sedang Dikemas", retrieve the message from Firestore
                getPesanKepadaPenjualFromFirestore(order) { pesan ->
                    order.pesanKepadaPenjual = pesan ?: "Tidak ada pesan"

                    // Update order status to "Pesanan Selesai"
                    order.statusOrder = "Pesanan Selesai"
                    order.metodePembayaran = metodePembayaran
                    updateOrderStatus(
                        order,
                        "Pesanan Selesai", // Final status
                        metodePembayaran,
                        order.pesanKepadaPenjual
                    )
                }
                showReviewAlert(order)
            }

            // Remove order after updating status
            orders.remove(order)
        }

        // Refresh the cart after confirmation
        loadCartItems()

        // Make sure the cancel button is still visible after confirmation
        binding.confirmButton.visibility = View.GONE
        binding.cancelButton.visibility = View.VISIBLE
        updateButtonVisibility()
        updateBottomLayout(itemCount, totalPrice)
    }

    private fun showReviewAlert(order: Order) {
        AlertDialog.Builder(this)
            .setTitle("Ulasan Produk")
            .setMessage("Apakah Anda ingin memberikan ulasan dan rating untuk produk ini?")
            .setPositiveButton("Ya") { _, _ ->
                // Arahkan ke halaman UlasanActivity
                val intent = Intent(this, UlasanActivity::class.java)
                intent.putExtra("PRODUCT_NAME", order.productName) // Mengirim productName ke UlasanActivity
                intent.putExtra("PRODUCT_ID", order.productId) // Mengirim productId ke UlasanActivity
                startActivity(intent)
            }
            .setNegativeButton("Tidak", null)
            .show()
    }

    // Function to get the "pesanKepadaPenjual" from Firestore when the status is "Pesanan Sedang Dikemas"
    private fun getPesanKepadaPenjualFromFirestore(order: Order, callback: (String?) -> Unit) {
        val docRef = db.collection("carts").document(order.orderNumber)
        docRef.get()
            .addOnSuccessListener { document ->
                val pesan = document.getString("pesanKepadaPenjual")
                callback(pesan)
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreError", "Error fetching pesan: ${e.message}")
                callback(null)
            }
    }

    private fun onCancelOrder() {
        val metodePembayaran = "Bayar Ditempat"
        val pesanKepadaPenjual = binding.edittextPesan.text.toString().trim()

        if (pesanKepadaPenjual.isEmpty()) {
            binding.edittextPesan.error = "Pesan tidak boleh kosong"
            return
        }

        orders.forEach { order ->
            // Update the status to "Pesanan Dibatalkan"
            order.statusOrder = "Pesanan Dibatalkan"
            order.metodePembayaran = metodePembayaran
            order.pesanKepadaPenjual = pesanKepadaPenjual

            // Update the order in Firestore
            updateOrderStatus(
                order,
                "Pesanan Dibatalkan",
                metodePembayaran,
                pesanKepadaPenjual
            )
        }

        // Keep the cancel button visible even after cancellation
        binding.confirmButton.visibility = View.GONE
        binding.cancelButton.visibility = View.VISIBLE
        loadCartItems() // Reload the cart items after the cancellation

        // Update UI after the status change
        adapter.notifyDataSetChanged()
        updateButtonVisibility()
        updateBottomLayout(itemCount, totalPrice)
    }

    private fun updateButtonVisibility() {
        val hasPendingOrders = orders.any {
            it.statusOrder == "Memesan" ||
                    it.statusOrder == "Pesanan Sedang Dikemas"
        }
        val hasReadyForCompletion = orders.any { it.statusOrder == "Pesanan Sedang Dikemas" }

        // Ensure cancel button stays visible even after confirmation, if the status is not completed or cancelled
        if (orders.any { it.statusOrder == "Menunggu Konfirmasi Penjual" || it.statusOrder == "Pesanan Sedang Dikemas" }) {
            binding.cancelButton.visibility = View.VISIBLE
        } else {
            binding.cancelButton.visibility = View.GONE
        }

        if (hasPendingOrders || hasReadyForCompletion) {
            binding.confirmButton.visibility = View.VISIBLE
        } else {
            binding.confirmButton.visibility = View.GONE
        }
    }

    fun updateBottomLayout(itemCount: Int, totalPrice: Double) {
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
