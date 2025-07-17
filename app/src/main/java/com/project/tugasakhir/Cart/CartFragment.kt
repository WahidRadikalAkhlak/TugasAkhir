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
    private val binding
        get() = _binding
            ?: throw IllegalStateException("Binding harus diinisialisasi sebelum dipakai!")

    private val orders = mutableListOf<Order>()
    private lateinit var adapter: OrderAdapter
    private val products =
        mutableListOf<Product>()  // Declare the products list to hold product data

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var selectedChip: String = "ALL"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvPesan.layoutManager = LinearLayoutManager(requireContext())
        adapter = OrderAdapter(orders, products, { selectedOrder ->
            openOrderDetail(selectedOrder)
        }, { orderToDelete ->
            hapusPesanan(orderToDelete)
        }, isForKeranjangPesanan = false)

        binding.rvPesan.adapter = adapter
        loadUserOrders()
        loadProducts() // Load products as well
        setupChipFilters()
    }

    private fun setupChipFilters() {
        binding.chip2.setOnClickListener { selectChip("Memesan") }
        binding.chip3.setOnClickListener { selectChip("Menunggu Konfirmasi Penjual") }
        binding.chip4.setOnClickListener { selectChip("Pesanan Sedang Dikemas") }
        binding.chip5.setOnClickListener { selectChip("Pesanan Selesai") }
        binding.chip6.setOnClickListener { selectChip("Pesanan Dibatalkan") }
    }

    private fun selectChip(status: String) {
        selectedChip = status
        filterOrdersByChip()
    }

    private fun filterOrdersByChip() {
        val filteredOrders = when (selectedChip) {
            "Memesan" -> orders.filter { it.statusOrder == "Memesan" }
            "Menunggu Konfirmasi Penjual" -> orders.filter { it.statusOrder == "Menunggu Konfirmasi Penjual" }
            "Pesanan Sedang Dikemas" -> orders.filter { it.statusOrder == "Pesanan Sedang Dikemas" }
            "Pesanan Selesai" -> orders.filter { it.statusOrder == "Pesanan Selesai" }
            "Pesanan Dibatalkan" -> orders.filter { it.statusOrder == "Pesanan Dibatalkan" }
            else -> orders  // Show all orders for "ALL"
        }
        adapter.updateList(filteredOrders)
    }
    private fun loadUserOrders() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            if (isAdded) {
                Toast.makeText(requireContext(), "User belum login", Toast.LENGTH_SHORT).show()
            }
            return
        }

        db.collection("carts")
            .whereEqualTo("userId", currentUser.uid)
            .addSnapshotListener { documents, error ->
                if (error != null) {
                    if (isAdded) {
                        Toast.makeText(
                            requireContext(),
                            "Error loading orders: ${error.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    return@addSnapshotListener
                }

                // Correctly checking if the documents are not empty
                if (documents != null && documents.size() > 0) {
                    orders.clear()
                    for (doc in documents) {
                        val order = doc.toObject(Order::class.java).apply {
                            docId = doc.id
                        }

                        // Handle product images
                        order.imageUrls = doc.get("imageUrls") as? List<String> ?: emptyList()

                        orders.add(order)
                    }
                    adapter.notifyDataSetChanged()
                } else {
                    if (isAdded) {
                        Toast.makeText(
                            requireContext(),
                            "Tidak ada item dalam keranjang",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
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
                Toast.makeText(
                    requireContext(),
                    "Gagal memuat data produk: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun hapusPesanan(order: Order) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "User belum login", Toast.LENGTH_SHORT).show()
            Log.d("KeranjangPesanan", "User not logged in.")
            return
        }

        if (order.orderNumber.isEmpty()) {
            Toast.makeText(requireContext(), "Order Number tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        Log.d("KeranjangPesanan", "Attempting to delete order with orderNumber: ${order.orderNumber}")

        // Hapus order dari koleksi 'carts'
        val cartRef = db.collection("carts")
            .whereEqualTo("orderNumber", order.orderNumber)
            .limit(1)  // Pastikan hanya satu dokumen yang ditemukan
            .get()
            .addOnSuccessListener { querySnapshot ->
                Log.d("KeranjangPesanan", "Query Firestore berhasil: ${querySnapshot.size()} documents found.")
                if (!querySnapshot.isEmpty) {
                    val docId = querySnapshot.documents[0].id
                    db.collection("carts").document(docId).delete()
                        .addOnSuccessListener {
                            Toast.makeText(requireContext(), "Pesanan berhasil dihapus", Toast.LENGTH_SHORT).show()
                            loadUserOrders()  // Reload orders setelah penghapusan
                        }
                        .addOnFailureListener { e ->
                            Log.e("Delete Order", "Error deleting order: ${e.message}")
                            Toast.makeText(requireContext(), "Gagal menghapus pesanan", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(requireContext(), "Pesanan tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Delete Order", "Error querying Firestore: ${e.message}")
                Toast.makeText(requireContext(), "Gagal menghapus pesanan", Toast.LENGTH_SHORT).show()
            }

        // Hapus produk yang terkait, jika ada
        val productRef = db.collection("products")
            .whereEqualTo("orderNumber", order.orderNumber)

        productRef.get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Log.d("KeranjangPesanan", "No related products found for order: ${order.orderNumber}")
                }
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
        val intent = Intent(context, KeranjangPesananActivity::class.java)
        intent.putExtra("ORDER_NUMBER", order.orderNumber) // Mengirim orderNumber yang dipilih
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Avoid memory leaks
    }
}