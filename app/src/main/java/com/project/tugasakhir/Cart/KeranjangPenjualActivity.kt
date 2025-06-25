package com.project.tugasakhir.Cart

import android.content.Intent
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
import com.project.tugasakhir.databinding.ActivityKeranjangPenjualBinding

class KeranjangPenjualActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKeranjangPenjualBinding

    private val orders = mutableListOf<Order>()
    private lateinit var adapter: OrderAdapter

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKeranjangPenjualBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup RecyclerView
        binding.rvPesan.layoutManager = LinearLayoutManager(this)
        adapter = OrderAdapter(orders, { selectedOrder ->
            openOrderDetail(selectedOrder)
        }, { orderToDelete ->
            hapusPesanan(orderToDelete)
        }, isForKeranjangPesanan = false)
        binding.rvPesan.adapter = adapter

        loadUserOrders()
    }

    private fun loadUserOrders() {
        binding.progressbarSettings.visibility = View.VISIBLE
        val currentUser = auth.currentUser
        if (currentUser == null) {
            binding.progressbarSettings.visibility = View.GONE
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("carts")
            .document(currentUser.uid)
            .collection("items")
            .get()
            .addOnSuccessListener { documents ->
                binding.progressbarSettings.visibility = View.GONE
                if (documents.isEmpty) {
                    Toast.makeText(this, "Tidak ada item dalam keranjang", Toast.LENGTH_SHORT).show()
                } else {
                    orders.clear()  // Clear the previous orders
                    for (doc in documents) {
                        val order = doc.toObject(Order::class.java).apply {
                            docId = doc.id
                        }
                        orders.add(order)
                    }
                    adapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener { e ->
                binding.progressbarSettings.visibility = View.GONE
                Toast.makeText(this, "Gagal memuat data pesanan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun hapusPesanan(order: Order) {
        val currentUser = auth.currentUser ?: run {
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        if (order.docId.isEmpty()) {
            Toast.makeText(this, "ID pesanan tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        val docRef = db.collection("carts").document(currentUser.uid).collection("items").document(order.docId)

        docRef.delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Pesanan berhasil dihapus", Toast.LENGTH_SHORT).show()
                loadUserOrders()  // Reload the orders after deletion
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menghapus pesanan: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        // Menghapus produk terkait jika diperlukan
        val productRef = db.collection("products")
            .whereEqualTo("orderNumber", order.docId)
        productRef.get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    db.collection("products").document(doc.id).delete()
                        .addOnSuccessListener {
                            Log.d("KeranjangPesanan", "Produk berhasil dihapus dari produk")
                        }
                        .addOnFailureListener { e ->
                            Log.e("KeranjangPesanan", "Gagal menghapus produk dari produk: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("KeranjangPesanan", "Gagal mencari produk terkait: ${e.message}")
            }
    }

    private fun openOrderDetail(order: Order) {
        val intent = Intent(this, KeranjangPesananActivity::class.java)
        intent.putExtra("order_data", order)  // Pass selected order to the next activity
        startActivity(intent)
    }
}
