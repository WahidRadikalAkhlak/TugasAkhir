package com.project.tugasakhir.Cart

import android.content.Intent
import android.os.Bundle
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
import com.project.tugasakhir.databinding.ActivityKeranjangPenjualBinding

class KeranjangPenjualActivity : AppCompatActivity() {

    private lateinit var binding: ActivityKeranjangPenjualBinding
    private lateinit var adapter: OrderAdapter
    private val orders = mutableListOf<Order>()
    private val products = mutableListOf<Product>()
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var selectedChip: String = "ALL"  // Default filter for all orders

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityKeranjangPenjualBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup RecyclerView
        binding.rvPesan.layoutManager = LinearLayoutManager(this)
        adapter = OrderAdapter(orders, products, { selectedOrder -> openOrderDetail(selectedOrder) }, { orderToDelete -> hapusPesanan(orderToDelete) }, isForKeranjangPesanan = false)
        binding.rvPesan.adapter = adapter

        binding.progressBar.visibility = View.VISIBLE  // Show progress bar when loading
        loadSellerOrders()  // Load orders for the seller
        loadProducts()  // Load the products to match with orders

        // Chip filter setup
        setupChipFilter()
    }

    private fun loadSellerOrders() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            binding.progressBar.visibility = View.GONE
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        // Show progress bar while loading data
        binding.progressBar.visibility = View.VISIBLE

        // Clear previous orders data before loading new data
        orders.clear()

        // Create the Firestore query with optional filtering based on the chip selection
        var query = db.collection("carts")
            .whereEqualTo("sellerUID", currentUser.uid)

        // Apply filter only if the chip is not "ALL"
        if (selectedChip != "ALL") {
            query = query.whereEqualTo("statusOrder", selectedChip)
        }

        // Fetch the data with snapshot listener
        query.addSnapshotListener { documents, error ->
            binding.progressBar.visibility = View.GONE // Hide progress bar after loading

            if (error != null) {
                Toast.makeText(this, "Error loading orders: ${error.message}", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            // Check if documents are present
            if (documents != null && documents.size() > 0) {
                orders.clear()
                for (doc in documents) {
                    val order = doc.toObject(Order::class.java).apply {
                        docId = doc.id
                    }
                    val product = products.find { it.productId == order.productId }
                    order.imageUrls = product?.imageUrls ?: emptyList()
                    orders.add(order)
                }
                adapter.notifyDataSetChanged()  // Refresh the UI
            } else {
                // If no orders, display a toast message and keep the chip active
                Toast.makeText(this, "Tidak ada item dalam keranjang untuk status $selectedChip", Toast.LENGTH_SHORT).show()
                adapter.notifyDataSetChanged()  // Refresh the UI with empty data
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
                Toast.makeText(this, "Gagal memuat data produk: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Chip filter setup to filter orders based on their status
    private fun setupChipFilter() {
        binding.chipgrp2.setOnCheckedChangeListener { group, checkedId ->
            // Set the selected chip based on which chip is selected
            selectedChip = when (checkedId) {
                R.id.chip2 -> "Memesan"
                R.id.chip3 -> "Menunggu Konfirmasi Penjual"
                R.id.chip4 -> "Pesanan Sedang Dikemas"
                R.id.chip5 -> "Pesanan Selesai"
                R.id.chip6 -> "Pesanan Dibatalkan"
                else -> "ALL" // Default is ALL
            }
            loadSellerOrders()  // Reload orders when chip is selected
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
                Toast.makeText(this, "Gagal menghapus pesanan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openOrderDetail(order: Order) {
        val intent = Intent(this, TerimaPesananActivity::class.java)
        intent.putExtra("ORDER_NUMBER", order.orderNumber) // Mengirim orderNumber yang dipilih
        startActivity(intent)
    }
}
