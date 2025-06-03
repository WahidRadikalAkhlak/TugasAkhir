package com.project.tugasakhir.Cart

import android.content.res.ColorStateList
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Adapter.OrderAdapter
import com.project.tugasakhir.Data.Order
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ActivityKeranjangPesananBinding
import kotlin.random.Random

class KeranjangPesananActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKeranjangPesananBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val orders = mutableListOf<Order>()
    private lateinit var adapter: OrderAdapter

    // Data dinamis
    private var itemCount = 0
    private var totalPrice = 0.0
    private var orderNumber = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKeranjangPesananBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = OrderAdapter(orders) { selectedOrder ->
            Toast.makeText(this, "Order dipilih: ${selectedOrder.userName}", Toast.LENGTH_SHORT).show()
        }
        binding.rvProdukKeranjang.layoutManager = LinearLayoutManager(this)
        binding.rvProdukKeranjang.adapter = adapter

        // Ambil nama pengguna dan tampilkan
        loadUserName()

        // Generate nomor order secara random
        orderNumber = generateOrderNumber()
        binding.nomorOrder.text = orderNumber

        // Load produk yang sudah di checkout di keranjang user saat ini
        loadCartItems()

        binding.confirmButton.backgroundTintList =
            ColorStateList.valueOf(resources.getColor(R.color.btn_color, null))
        binding.cancelButton.backgroundTintList =
            ColorStateList.valueOf(resources.getColor(R.color.btn_color, null))

        binding.confirmButton.setOnClickListener {
            if (itemCount <= 0) {
                Toast.makeText(this, "Tidak ada item untuk diproses", Toast.LENGTH_SHORT).show()
            } else {
                onConfirmOrder()
            }
        }

        binding.cancelButton.setOnClickListener {
            onCancelOrder()
        }
    }

    private fun loadUserName() {
        val currentUser = auth.currentUser
        if (currentUser != null) {
            // Jika displayName tersedia gunakan, jika tidak gunakan email sebagai fallback
            val displayName = currentUser.displayName ?: currentUser.email ?: "Pengguna"
            binding.namaPengguna.text = displayName
        } else {
            binding.namaPengguna.text = "Pengguna belum login"
        }
    }

    private fun generateOrderNumber(): String {
        // Contoh format ORD + timestamp acak
        val timestamp = System.currentTimeMillis()
        val randomNum = Random.nextInt(1000, 9999)
        return "ORD$timestamp$randomNum"
    }

    private fun loadCartItems() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("carts")
            .document(currentUser.uid)
            .collection("items")
            .get()
            .addOnSuccessListener { documents ->
                orders.clear()
                totalPrice = 0.0
                itemCount = 0

                for (doc in documents) {
                    val order = doc.toObject(Order::class.java)
                    orders.add(order)
                    itemCount += order.quantity
                    totalPrice += order.totalPrice
                }

                adapter.notifyDataSetChanged()
                updateBottomLayout(itemCount, totalPrice)
            }
            .addOnFailureListener {
                Toast.makeText(this, "Gagal memuat data keranjang: ${it.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateBottomLayout(itemCount: Int, totalPrice: Double) {
        binding.itemCount.text = "Item: $itemCount"
        binding.HargaBarang.text = "Rp ${String.format("%,.0f", totalPrice)}"
    }

    private fun onConfirmOrder() {
        if (orders.isEmpty()) {
            Toast.makeText(this, "Keranjang kosong, tidak bisa konfirmasi pesanan", Toast.LENGTH_SHORT).show()
            return
        }

        val currentUser = auth.currentUser ?: run {
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        val batch = db.batch()
        val orderDocRef = db.collection("orders").document()  // buat dokumen order baru dengan ID auto

        // Data utama order
        val orderData = hashMapOf(
            "orderNumber" to orderNumber,
            "userId" to currentUser.uid,
            "userName" to (currentUser.displayName ?: currentUser.email ?: "User"),
            "itemCount" to itemCount,
            "totalPrice" to totalPrice,
            "status" to "Memesan",
            "orderDate" to getCurrentDateString(),
            "orderTime" to getCurrentTimeString(),
            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )

        // Simpan data order utama
        batch.set(orderDocRef, orderData)

        // Simpan tiap item order di subcollection "items"
        val itemsCollectionRef = orderDocRef.collection("items")
        for (order in orders) {
            val itemDocRef = itemsCollectionRef.document()
            val itemData = hashMapOf(
                "productName" to order.productName,
                "productType" to order.productType,
                "quantity" to order.quantity,
                "pricePerUnit" to order.pricePerUnit,
                "totalPrice" to order.totalPrice,
                "imageUrl" to order.imageUrl,
                "userName" to order.userName,
                "userAddress" to order.userAddress,
                "status" to "Memesan"
            )
            batch.set(itemDocRef, itemData)
        }

        // Jalankan batch write
        batch.commit()
            .addOnSuccessListener {
                // Setelah order tersimpan, hapus keranjang user
                clearUserCart(currentUser.uid)

                Toast.makeText(
                    this,
                    "Pesanan berhasil dikonfirmasi!",
                    Toast.LENGTH_LONG
                ).show()

                // Update UI atau kembali ke halaman sebelumnya
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Gagal menyimpan pesanan: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun clearUserCart(userId: String) {
        val cartItemsRef = db.collection("carts").document(userId).collection("items")
        cartItemsRef.get()
            .addOnSuccessListener { documents ->
                val batch = db.batch()
                for (doc in documents) {
                    batch.delete(doc.reference)
                }
                batch.commit()
                    .addOnSuccessListener {
                        // Keranjang berhasil dikosongkan
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Gagal mengosongkan keranjang: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal mengambil data keranjang: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Fungsi tambahan format tanggal dan waktu
    private fun getCurrentDateString(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd")
        return sdf.format(java.util.Date())
    }

    private fun getCurrentTimeString(): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss")
        return sdf.format(java.util.Date())
    }

    private fun onCancelOrder() {
        Toast.makeText(
            this,
            "Proses pesanan dibatalkan",
            Toast.LENGTH_SHORT
        ).show()
        // TODO: Reset UI atau hapus data sementara jika perlu
    }
}
