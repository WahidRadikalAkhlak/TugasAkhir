package com.project.tugasakhir.Cart

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Account.Penjual.TerimaPesananActivity
import com.project.tugasakhir.Adapter.OrderAdapter
import com.project.tugasakhir.Data.Order
import com.project.tugasakhir.Data.Product
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ActivityKeranjangPenjualBinding

class KeranjangPenjualActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKeranjangPenjualBinding
    private val orders = mutableListOf<Order>()
    private lateinit var adapter: OrderAdapter
    private val products = mutableListOf<Product>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKeranjangPenjualBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.rvPesan.layoutManager = LinearLayoutManager(this)
        adapter = OrderAdapter(orders, products, { selectedOrder ->
            openOrderDetail(selectedOrder)
        }, { orderToDelete ->
            hapusPesanan(orderToDelete)
        }, isForKeranjangPesanan = false)

        binding.rvPesan.adapter = adapter

        loadSellerOrders() // Load orders from Firestore
        loadProducts() // Load products to match with orders
    }

    private fun loadSellerOrders() {
        binding.progressbarSettings.visibility = View.VISIBLE
        val currentUser = auth.currentUser
        if (currentUser == null) {
            binding.progressbarSettings.visibility = View.GONE
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        // Ambil nama penjual dari pengguna yang sedang login
        val sellerName = currentUser.displayName ?: ""

        // Pastikan sellerName tidak kosong
        if (sellerName.isEmpty()) {
            binding.progressbarSettings.visibility = View.GONE
            Toast.makeText(this, "Nama penjual tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        // Query untuk mengambil pesanan berdasarkan sellerName di subkoleksi 'items'
        db.collection("carts")
            .document(currentUser.uid) // Mengakses cart berdasarkan user
            .collection("items") // Mengakses subkoleksi items dalam cart
            .whereEqualTo("sellerUserName", sellerName) // Menyaring berdasarkan sellerUserName di dalam subkoleksi items
            .get()
            .addOnSuccessListener { itemDocuments ->
                binding.progressbarSettings.visibility = View.GONE
                if (itemDocuments.isEmpty) {
                    Toast.makeText(this, "Tidak ada item dalam keranjang", Toast.LENGTH_SHORT).show()
                } else {
                    orders.clear() // Kosongkan daftar pesanan sebelumnya
                    // Loop melalui setiap dokumen di koleksi carts
                    for (doc in itemDocuments) {
                        val order = doc.toObject(Order::class.java).apply {
                            docId = doc.id  // Menyimpan ID dokumen untuk setiap pesanan
                        }
                        orders.add(order)  // Menambahkan pesanan ke daftar
                    }
                    adapter.notifyDataSetChanged()  // Notify adapter untuk memperbarui RecyclerView
                }
            }
            .addOnFailureListener { e ->
                binding.progressbarSettings.visibility = View.GONE
                Toast.makeText(this, "Gagal memuat data pesanan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadProducts() {
        db.collection("products")
            .get()
            .addOnSuccessListener { documents ->
                products.clear()  // Kosongkan daftar produk sebelumnya
                for (doc in documents) {
                    val product = doc.toObject(Product::class.java)
                    products.add(product)  // Menambahkan produk ke daftar
                }
                adapter.notifyDataSetChanged()  // Notify adapter untuk memperbarui RecyclerView
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load products: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun hapusPesanan(order: Order) {
        val currentUser = auth.currentUser ?: run {
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        val docRef = db.collection("carts")
            .document(currentUser.uid)
            .collection("items")
            .document(order.docId)

        docRef.delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Pesanan berhasil dihapus", Toast.LENGTH_SHORT).show()
                loadSellerOrders()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menghapus pesanan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openOrderDetail(order: Order) {
        val intent = Intent(this, TerimaPesananActivity::class.java)
        intent.putExtra("order_data", order)
        startActivity(intent)
    }
}
