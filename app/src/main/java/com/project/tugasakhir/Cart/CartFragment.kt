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
    private val products = mutableListOf<Product>()  // Declare the products list to hold product data

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

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
    }

    private fun loadUserOrders() {
        if (!isAdded) {
            Log.e("CartFragment", "Fragment belum terpasang ke Activity!")
            return
        }

        binding.progressbarSettings.visibility = View.VISIBLE
        val currentUser = auth.currentUser
        if (currentUser == null) {
            binding.progressbarSettings.visibility = View.GONE
            Toast.makeText(requireContext(), "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("carts")
            .document(currentUser.uid)
            .collection("items")
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded) {
                    Log.e("CartFragment", "Fragment tidak lagi terpasang saat callback dipanggil!")
                    return@addOnSuccessListener
                }
                binding.progressbarSettings.visibility = View.GONE
                if (documents.isEmpty) {
                    Toast.makeText(requireContext(), "Tidak ada item dalam keranjang", Toast.LENGTH_SHORT).show()
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
                if (isAdded) {
                    binding.progressbarSettings.visibility = View.GONE
                    Toast.makeText(requireContext(), "Gagal memuat data pesanan: ${e.message}", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(requireContext(), "Gagal memuat data produk: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun hapusPesanan(order: Order) {
        val currentUser = auth.currentUser ?: run {
            Toast.makeText(requireContext(), "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        if (order.docId.isEmpty()) { // Periksa docId, bukan orderNumber
            Toast.makeText(requireContext(), "ID pesanan tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        val docRef = db.collection("carts").document(currentUser.uid).collection("items").document(order.docId)

        docRef.delete()
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Pesanan berhasil dihapus", Toast.LENGTH_SHORT).show()
                loadUserOrders()  // Reload the orders after deletion
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal menghapus pesanan: ${e.message}", Toast.LENGTH_SHORT).show()
            }

        // Menghapus produk terkait jika diperlukan
        val productRef = db.collection("products")
            .whereEqualTo("orderNumber", order.docId) // Ganti orderNumber dengan docId
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
        val intent = Intent(context, KeranjangPesananActivity::class.java)
        intent.putExtra("order_data", order)  // Pass selected order to the next activity
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Avoid memory leaks
    }
}
