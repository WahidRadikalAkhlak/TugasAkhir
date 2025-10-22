package com.project.tugasakhir.Cart

import android.content.Intent
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
import com.project.tugasakhir.Chat.pesan.PesanActivity
import com.project.tugasakhir.Data.Message
import com.project.tugasakhir.Data.Order
import com.project.tugasakhir.Data.Product
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ActivityTerimaPesananBinding

class TerimaPesananActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTerimaPesananBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val orders = mutableListOf<Order>()
    private lateinit var adapter: OrderAdapter
    private val products = mutableListOf<Product>()
    private lateinit var selectedOrderNumber: String
    private var itemCount = 0
    private var totalPrice = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTerimaPesananBinding.inflate(layoutInflater)
        setContentView(binding.root)

        selectedOrderNumber = intent.getStringExtra("ORDER_NUMBER") ?: ""
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

        // In TerimaPesananActivity
        binding.rvProdukKeranjang.layoutManager = LinearLayoutManager(this)
        binding.rvProdukKeranjang.adapter = adapter

        loadUserName()
        loadCartItems()
        loadProducts() // Load products as well

        // Buttons configuration
        binding.confirmButton.backgroundTintList =
            ColorStateList.valueOf(resources.getColor(R.color.btn_color, null))
        binding.tolakTawaran.backgroundTintList =
            ColorStateList.valueOf(resources.getColor(R.color.btn_color, null))

        binding.confirmButton.setOnClickListener {
            if (itemCount <= 0) {
                Toast.makeText(this, "Tidak ada item untuk diproses", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Confirming order", Toast.LENGTH_SHORT).show()
                onConfirmOrder()
            }
            binding.progressBar.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
        }

        binding.tolakTawaran.setOnClickListener {
            Toast.makeText(this, "Cancelling order", Toast.LENGTH_SHORT).show()
            onCancelOrder()
            binding.progressBar.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
        }
        binding.btnKirimPesan.setOnClickListener {
            sendMessageToUser()
            binding.progressBar.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
        }
    }

    private fun loadUserName() {
        val currentUser = auth.currentUser
        binding.namaPengguna.text = currentUser?.displayName ?: currentUser?.email ?: "Pengguna belum login"
    }

    private fun sendMessageToUser() {
        val currentUser = auth.currentUser ?: return

        // Mendapatkan userId dan orderNumber dari detail pesanan
        val userId = orders.firstOrNull()?.userId ?: return
        val orderNumber = orders.firstOrNull()?.orderNumber ?: return  // Mendapatkan orderNumber untuk query

        // Query Firestore untuk mendapatkan userName dari koleksi 'carts' berdasarkan orderNumber
        db.collection("carts").document(orderNumber).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Mendapatkan userName dari dokumen
                    val receiverName = document.getString("userName") ?: "User"

                    // Membentuk chatId menggunakan userId dan sellerId (currentUser.uid)
                    val chatId = createChatId(currentUser.uid, userId)

                    // Memeriksa apakah chat sudah ada di Firestore
                    val chatRef = db.collection("chats").document(chatId)

                    chatRef.get().addOnSuccessListener { document ->
                        if (document.exists()) {
                            // Jika chat sudah ada, kirim pesan "Terima Kasih" ke chat yang sudah ada
                            val message = Message(
                                senderId = currentUser.uid,
                                senderName = currentUser.displayName ?: "Unknown",
                                message = "Terima Kasih sudah memesan Produk kami",
                                timestamp = System.currentTimeMillis(),
                                receiverId = userId,
                                receiverName = receiverName,  // Menggunakan receiverName yang dinamis
                                chatId = chatId
                            )

                            // Mengirim pesan ke chat yang sudah ada
                            chatRef.collection("messages").add(message)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Pesan terkirim", Toast.LENGTH_SHORT).show()
                                    Log.d("Pesan", "Pesan terkirim ke chat yang sudah ada")

                                    // Membuka PesanActivity untuk melihat pesan
                                    openChatDetail(chatId)
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Gagal mengirim pesan: ${e.message}", Toast.LENGTH_SHORT).show()
                                    Log.e("Pesan", "Gagal mengirim pesan: ${e.message}")
                                }
                        } else {
                            // Jika chat belum ada, buat chat baru dan kirim pesan
                            val newChatId = createChatId(currentUser.uid, userId)

                            // Membuat dokumen chat baru
                            val chatData = hashMapOf(
                                "participants" to listOf(currentUser.uid, userId),
                                "timestamp" to System.currentTimeMillis(),
                            )

                            // Membuat chat dan mengirim pesan
                            db.collection("chats").document(newChatId).set(chatData)
                                .addOnSuccessListener {
                                    // Sekarang, tambahkan pesan ke chat baru
                                    val message = Message(
                                        senderId = currentUser.uid,
                                        senderName = currentUser.displayName ?: "Unknown",
                                        message = "Terima Kasih sudah memesan Produk kami",
                                        timestamp = System.currentTimeMillis(),
                                        receiverId = userId,
                                        receiverName = receiverName,
                                        chatId = newChatId
                                    )

                                    // Mengirim pesan pertama
                                    db.collection("chats").document(newChatId)
                                        .collection("messages").add(message)
                                        .addOnSuccessListener {
                                            Toast.makeText(this, "Pesan terkirim", Toast.LENGTH_SHORT).show()
                                            Log.d("Pesan", "Pesan terkirim ke chat baru")

                                            // Membuka PesanActivity untuk melihat pesan
                                            openChatDetail(newChatId)
                                        }
                                        .addOnFailureListener { e ->
                                            Toast.makeText(this, "Gagal mengirim pesan: ${e.message}", Toast.LENGTH_SHORT).show()
                                            Log.e("Pesan", "Gagal mengirim pesan: ${e.message}")
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Gagal membuat chat baru: ${e.message}", Toast.LENGTH_SHORT).show()
                                    Log.e("Pesan", "Gagal membuat chat baru: ${e.message}")
                                }
                        }
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, "Gagal memeriksa chat: ${e.message}", Toast.LENGTH_SHORT).show()
                        Log.e("Pesan", "Gagal memeriksa chat: ${e.message}")
                    }
                } else {
                    Toast.makeText(this, "User tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memuat username: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("Pesan", "Gagal memuat username: ${e.message}")
            }
    }

    // Fungsi untuk membuat chatId berdasarkan userId dan sellerId
    private fun createChatId(sellerId: String, userId: String): String {
        // Membuat chatId dengan mengurutkan userId dan sellerId, sehingga urutannya konsisten
        val ids = listOf(sellerId, userId).sorted()
        return "chat_${ids[0]}_${ids[1]}"  // Urutan yang konsisten memastikan chatId yang sama untuk kedua pengguna
    }

    private fun openChatDetail(chatId: String) {
        val intent = Intent(this, PesanActivity::class.java)
        intent.putExtra("chat_id", chatId)  // Mengirim chatId
        startActivity(intent)  // Membuka PesanActivity untuk melihat pesan
    }

    private fun loadCartItems() {
        val currentUser = auth.currentUser ?: run {
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        orders.clear()  // Clear old orders before fetching new ones
        db.collection("carts")
            .whereEqualTo("sellerUID", currentUser.uid) // Mengambil data berdasarkan sellerUID
            .whereEqualTo("orderNumber", selectedOrderNumber)
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
                // Membuat map untuk menyaring pesanan berdasarkan orderNumber
                val orderMap = mutableMapOf<String, Order>()

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
                        pesanKepadaPenjual =
                            doc.getString("pesanKepadaPenjual") ?: "Tidak ada pesan"
                    }

                    // Menambahkan pesanan ke dalam map jika orderNumber unik dan statusnya masih "Memesan"
                    if (order.productName.isNotEmpty() && order.productType.isNotEmpty()) {
                        if (!orderMap.containsKey(order.orderNumber)) {
                            orderMap[order.orderNumber] = order
                            itemCount += order.quantity
                            totalPrice += order.totalPrice
                        }
                    }
                }

                // Menambahkan pesanan yang sudah dipisahkan berdasarkan orderNumber ke dalam list orders
                orders.addAll(orderMap.values)

                // Update UI dengan pesanan pertama jika tersedia
                binding.namaPengguna.text = orders.firstOrNull()?.userName ?: "Nama tidak tersedia"
                binding.email.text = orders.firstOrNull()?.email ?: "Email tidak tersedia"
                binding.orderNumber.text =
                    orders.firstOrNull()?.orderNumber ?: "Order Number tidak tersedia"
                binding.statusOrder.text =
                    orders.firstOrNull()?.statusOrder ?: "Status Order tidak tersedia"
                binding.tanggalOrder.text =
                    orders.firstOrNull()?.orderDate ?: "Order Date tidak tersedia"
                binding.orderTime.text =
                    orders.firstOrNull()?.orderTime ?: "Order Time tidak tersedia"

                // Display the message from Firestore in the TextView
                binding.edittextPesanDariPenjual.text = orders.firstOrNull()?.pesanKepadaPenjual

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
        val pesanKepadaPenjual = binding.edittextPesanDariPenjual.text.toString().trim()

        if (pesanKepadaPenjual.isEmpty()) {
            binding.edittextPesanDariPenjual.error = "Pesan tidak boleh kosong"
            return
        }

        orders.forEach { order ->
            // Update the status to "Pesanan Sedang Dikemas" when seller confirms
            order.statusOrder = "Pesanan Sedang Dikemas"
            order.metodePembayaran = metodePembayaran
            order.pesanKepadaPenjual = pesanKepadaPenjual

            updateOrderStatus(
                order,
                "Pesanan Sedang Dikemas", // Update to "Pesanan Sedang Dikemas"
                metodePembayaran,
                pesanKepadaPenjual
            )
        }

        // Refresh the cart after seller confirmation
        loadCartItems()

        // Hide the button after seller confirmation
        binding.confirmButton.visibility = View.GONE
        binding.tolakTawaran.visibility = View.VISIBLE
        updateButtonVisibility()
        updateBottomLayout(itemCount, totalPrice)
    }


    private fun onCancelOrder() {
        val metodePembayaran = "Bayar Ditempat"
        val pesanKepadaPenjual = binding.edittextPesanDariPenjual.text.toString().trim()

        if (pesanKepadaPenjual.isEmpty()) {
            binding.edittextPesanDariPenjual.error = "Pesan tidak boleh kosong"
            return
        }

        orders.forEach { order ->
            order.statusOrder = "Pesanan Dibatalkan"
            order.metodePembayaran = metodePembayaran
            order.pesanKepadaPenjual = pesanKepadaPenjual

            // Update the order in Firestore
            updateOrderStatus(order, "Pesanan Dibatalkan", metodePembayaran, pesanKepadaPenjual)
        }

        binding.confirmButton.visibility = View.GONE
        binding.tolakTawaran.visibility = View.GONE
        loadCartItems() // Reload the cart items after the cancellation

        // Update UI after the status change
        adapter.notifyDataSetChanged()
        updateButtonVisibility()
        updateBottomLayout(itemCount, totalPrice)
    }

    private fun updateButtonVisibility() {
        // Check if there are orders with status "Menunggu Konfirmasi Pembelian Anda"
        val hasPendingOrders = orders.any { it.statusOrder == "Menunggu Konfirmasi Penjual" }

        if (hasPendingOrders) {
            // Show buttons if there are pending orders
            binding.confirmButton.visibility = View.VISIBLE
            binding.tolakTawaran.visibility = View.VISIBLE
        } else {
            // Hide buttons if there are no pending orders
            binding.confirmButton.visibility = View.GONE
            binding.tolakTawaran.visibility = View.GONE
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
                Toast.makeText(this, "Gagal menghapus pesanan: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

}