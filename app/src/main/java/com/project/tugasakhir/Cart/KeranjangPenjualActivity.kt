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
    private lateinit var adapter: OrderAdapter
    private val orders = mutableListOf<Order>()
    private val products = mutableListOf<Product>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKeranjangPenjualBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup RecyclerView
        binding.rvPesan.layoutManager = LinearLayoutManager(this)
        adapter = OrderAdapter(orders, products, { selectedOrder ->
            openOrderDetail(selectedOrder)
        }, { orderToDelete ->
            hapusPesanan(orderToDelete)
        }, isForKeranjangPesanan = false)

        binding.rvPesan.adapter = adapter

        loadSellerOrders() // Load orders for the seller
        loadProducts() // Load the products to match with orders

        // Chip filter setup
        setupChipFilter()
    }

    private fun loadSellerOrders() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            binding.progressbarSettings.visibility = View.GONE
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("carts")
            .whereEqualTo("sellerUID", currentUser.uid)
            .addSnapshotListener { documents, error ->
                if (error != null) {
                    Toast.makeText(
                        this,
                        "Error loading orders: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addSnapshotListener
                }

                if (documents != null && !documents.isEmpty) {
                    orders.clear()  // Clear previous orders
                    for (doc in documents) {
                        val order = doc.toObject(Order::class.java).apply {
                            docId = doc.id
                        }
                        orders.add(order)
                    }
                    adapter.notifyDataSetChanged()  // Refresh the UI
                } else {
                    Toast.makeText(this, "Tidak ada item dalam keranjang", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    // Function to load products from Firestore to match with orders
    private fun loadProducts() {
        db.collection("products")
            .get()
            .addOnSuccessListener { documents ->
                products.clear() // Clear previous products list
                for (doc in documents) {
                    val product = doc.toObject(Product::class.java)
                    products.add(product) // Add each product to the list
                }
                adapter.notifyDataSetChanged() // Notify adapter that the product list is ready
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memuat data produk: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    // Chip filter setup to filter orders based on their status
    private fun setupChipFilter() {
        binding.chipgrp2.setOnCheckedChangeListener { group, checkedId ->
            val filteredOrders = when (checkedId) {
                R.id.chip2 -> orders.filter { it.statusOrder == "Memesan" } // Order in "Memesan" status
                R.id.chip3 -> orders.filter { it.statusOrder == "Konfirmasi" } // Order in "Konfirmasi" status
                R.id.chip4 -> orders.filter { it.statusOrder == "Selesai" } // Order in "Selesai" status
                else -> orders // Show all orders when "Semua" is selected
            }
            orders.clear()
            orders.addAll(filteredOrders)
            adapter.notifyDataSetChanged()
        }
    }

    private fun hapusPesanan(order: Order) {
        val currentUser = auth.currentUser ?: run {
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        val orderRef = db.collection("carts")
            .document(order.orderNumber)  // Directly reference the order document by orderNumber

        orderRef.delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Pesanan berhasil dihapus", Toast.LENGTH_SHORT).show()
                loadSellerOrders()  // Reload the seller's orders
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menghapus pesanan: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun openOrderDetail(order: Order) {
        val intent = Intent(this, TerimaPesananActivity::class.java)
        intent.putExtra("order_data", order)  // Pass selected order to the next activity
        startActivity(intent)
    }
}