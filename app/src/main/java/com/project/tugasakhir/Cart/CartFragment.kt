package com.project.tugasakhir.Cart

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Adapter.OrderAdapter
import com.project.tugasakhir.Data.Order
import com.project.tugasakhir.Data.Product
import com.project.tugasakhir.databinding.FragmentCartBinding

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding harus diinisialisasi sebelum dipakai!")

    private val orders = mutableListOf<Order>()
    private lateinit var adapter: OrderAdapter
    private val products = mutableListOf<Product>()

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var selectedChip: String = "ALL"  // Default filter for all orders

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up RecyclerView and Adapter
        binding.rvPesan.layoutManager = LinearLayoutManager(requireContext())
        adapter = OrderAdapter(orders, products, { selectedOrder -> openOrderDetail(selectedOrder) }, { orderToDelete -> hapusPesanan(orderToDelete) }, isForKeranjangPesanan = false)
        binding.rvPesan.adapter = adapter

        binding.progressBar.visibility = View.VISIBLE  // Show progress bar when loading
        loadUserOrders()  // Load orders initially
        loadProducts()    // Load products for the cart
        setupChipFilters()  // Initialize chip filters
    }

    // Set up Chip filters
    private fun setupChipFilters() {
        binding.chip2.setOnClickListener { selectChip("Memesan") }
        binding.chip3.setOnClickListener { selectChip("Menunggu Konfirmasi Penjual") }
        binding.chip4.setOnClickListener { selectChip("Pesanan Sedang Dikemas") }
        binding.chip5.setOnClickListener { selectChip("Pesanan Selesai") }
        binding.chip6.setOnClickListener { selectChip("Pesanan Dibatalkan") }
        binding.chip1.setOnClickListener { selectChip("ALL") }  // Chip for all orders
    }

    private fun selectChip(status: String) {
        selectedChip = status
        Log.d("Selected Chip", "Selected Status: $selectedChip")
        loadUserOrders()  // Reload orders based on chip selection
    }

    private fun loadUserOrders() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        // Show progress bar while loading data
        binding.progressBar.visibility = View.VISIBLE

        // Clear previous orders data before loading new data
        orders.clear()

        // Create the Firestore query
        var query = db.collection("carts").whereEqualTo("userId", currentUser.uid)

        // Apply filter only if the chip is not "ALL"
        if (selectedChip != "ALL") {
            query = query.whereEqualTo("statusOrder", selectedChip)
        }

        // Fetch the data with snapshot listener
        query.addSnapshotListener { documents, error ->
            binding.progressBar.visibility = View.GONE // Hide progress bar once data is loaded

            if (error != null) {
                Log.e("Firestore Error", "Error loading orders: ${error.message}")
                return@addSnapshotListener
            }

            // Check if documents are present
            if (documents != null && documents.size() > 0) {
                orders.clear()
                for (doc in documents) {
                    val order = doc.toObject(Order::class.java).apply {
                        docId = doc.id
                    }
                    order.imageUrls = doc.get("imageUrls") as? List<String> ?: emptyList()
                    orders.add(order)
                }
                adapter.notifyDataSetChanged()  // Update RecyclerView
                Log.d("Firestore Query", "Loaded ${orders.size} orders for status: $selectedChip")
            } else {
                Log.d("Firestore Query", "No orders found for status: $selectedChip")
                Toast.makeText(requireContext(), "Tidak ada item dalam keranjang", Toast.LENGTH_SHORT).show()
                // Clear the adapter if no data is found for the selected chip
                adapter.notifyDataSetChanged()
            }
        }
    }

    private fun loadProducts() {
        db.collection("products")
            .get()
            .addOnSuccessListener { documents ->
                products.clear()
                for (doc in documents) {
                    val product = doc.toObject(Product::class.java)
                    products.add(product)
                }
                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal memuat data produk: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun hapusPesanan(order: Order) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        if (order.orderNumber.isEmpty()) {
            Toast.makeText(requireContext(), "Order Number tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("carts")
            .whereEqualTo("orderNumber", order.orderNumber)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (!querySnapshot.isEmpty) {
                    val docId = querySnapshot.documents[0].id
                    db.collection("carts").document(docId).delete()
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Pesanan berhasil dihapus", Toast.LENGTH_SHORT).show()
                            loadUserOrders()  // Reload orders after deletion
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(requireContext(), "Gagal menghapus pesanan", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(requireContext(), "Pesanan tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal mencari pesanan", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openOrderDetail(order: Order) {
        val intent = Intent(context, KeranjangPesananActivity::class.java)
        intent.putExtra("ORDER_NUMBER", order.orderNumber)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
