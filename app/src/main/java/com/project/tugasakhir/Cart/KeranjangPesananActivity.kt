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

        orders.clear()  // Clear the old orders before fetching new ones

        db.collection("carts")
            .document(currentUser.uid) // User-specific cart
            .collection("items")
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
                        orderNumber = doc.getString("orderNumber") ?: ""  // Ensure this is fetched
                        orderDate = doc.getString("orderDate") ?: ""  // Ensure this is fetched
                        orderTime = doc.getString("orderTime") ?: ""  // Ensure this is fetched
                        statusOrder = doc.getString("statusOrder") ?: "Memesan"  // Ensure this is fetched
                        pricePerUnit = doc.getDouble("pricePerUnit") ?: 0.0
                        productName = doc.getString("productName") ?: ""
                        productType = doc.getString("productType") ?: ""
                        quantity = doc.getLong("quantity")?.toInt() ?: 0
                        totalPrice = doc.getDouble("totalPrice") ?: 0.0
                        timestamp = doc.getTimestamp("timestamp")

                        // Correctly fetch image URLs and base64 lists
                        imageUrls = (doc.get("imageUrls") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        imageBase64List = (doc.get("imageBase64List") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
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
        val pesanKepadaPenjual = binding.edittextPesan.text.toString().trim()

        // Jika pesan kosong, beri nilai default jika perlu
        if (pesanKepadaPenjual.isEmpty()) {
            binding.edittextPesan.error = "Pesan tidak boleh kosong"
            return
        }

        orders.forEach { order ->
            // Update status pesanan dan data lainnya, termasuk pesan kepada penjual
            order.statusOrder = "Menunggu Konfirmasi Pembelian Anda"
            order.metodePembayaran = metodePembayaran
            order.pesanKepadaPenjual = pesanKepadaPenjual

            // Update data pesanan ke Firestore
            updateOrderStatus(order, "Menunggu Konfirmasi Pembelian Anda", metodePembayaran, pesanKepadaPenjual)
        }

        binding.confirmButton.visibility = View.GONE
        binding.cancelButton.visibility = View.VISIBLE

        // Update UI untuk mencerminkan perubahan setelah konfirmasi
        updateUIWithOrderData()

        // Notify the adapter to update the list
        adapter.notifyDataSetChanged()
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
            // Update status pesanan dan data lainnya, termasuk pesan kepada penjual
            order.statusOrder = "Pesanan Dibatalkan"
            order.metodePembayaran = metodePembayaran
            order.pesanKepadaPenjual = pesanKepadaPenjual

            // Update data pesanan ke Firestore
            updateOrderStatus(order, "Pesanan Dibatalkan", metodePembayaran, pesanKepadaPenjual)
        }

        // Menyembunyikan tombol pembatalan setelah pesanan dibatalkan
        binding.confirmButton.visibility = View.GONE
        binding.cancelButton.visibility = View.VISIBLE

        // Update UI untuk mencerminkan perubahan setelah pembatalan
        updateUIWithOrderData()

        // Update UI untuk mencerminkan perubahan
        adapter.notifyDataSetChanged()
        updateButtonVisibility()
        updateBottomLayout(itemCount, totalPrice)
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

    private fun updateUIWithOrderData() {
        // Pastikan Anda memperbarui data yang sesuai dengan item yang pertama dalam daftar orders
        val firstOrder = orders.firstOrNull()
        if (firstOrder != null) {
            binding.email.text = firstOrder.email
            binding.orderNumber.text = firstOrder.orderNumber
            binding.statusOrder.text = firstOrder.statusOrder
            binding.tanggalOrder.text = firstOrder.orderDate
            binding.orderTime.text = firstOrder.orderTime
        } else {
            // In case orders are empty or not updated properly, set default text
            binding.email.text = "Email tidak tersedia"
            binding.orderNumber.text = "Order Number tidak tersedia"
            binding.statusOrder.text = "Status Order tidak tersedia"
            binding.tanggalOrder.text = "Order Date tidak tersedia"
            binding.orderTime.text = "Order Time tidak tersedia"
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

        // Referensi dokumen pesanan di Firestore
        val docRef = db.collection("carts")
            .document(currentUser.uid)
            .collection("items")
            .document(order.docId)

        // Update status pesanan, metode pembayaran, dan pesan kepada penjual di Firestore
        docRef.update(
            "statusOrder", newStatus,
            "metodePembayaran", metodePembayaran,
            "pesanKepadaPenjual", pesanKepadaPenjual
        ).addOnSuccessListener {
            Log.d("FirestoreUpdate", "Order successfully updated with status: $newStatus")
        }.addOnFailureListener {
            Toast.makeText(this, "Gagal update order", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hapusPesanan(order: Order) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        if (order.docId.isEmpty()) {
            Toast.makeText(this, "ID pesanan tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        val docRef = db.collection("carts")
            .document(currentUser.uid)
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

                // Reload cart items after deletion
                loadCartItems()  // Make sure to reload after deletion
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menghapus pesanan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

}