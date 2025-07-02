package com.project.tugasakhir.Account.Penjual

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

    private var itemCount = 0
    private var totalPrice = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTerimaPesananBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize RecyclerView and Adapter
        adapter = OrderAdapter(orders, products, { selectedOrder ->
            // Handle item click for order details
            Toast.makeText(this, "Order selected: ${selectedOrder.productName}", Toast.LENGTH_SHORT).show()
        }, { orderToDelete ->
            // Handle order deletion (if applicable)
            deleteOrder(orderToDelete)
        }, isForKeranjangPesanan = true)

        binding.rvProdukKeranjang.layoutManager = LinearLayoutManager(this)
        binding.rvProdukKeranjang.adapter = adapter

        // Load seller's orders
        loadSellerOrders()

        // Configure buttons and actions
        binding.confirmButton.setOnClickListener {
            if (itemCount <= 0) {
                Toast.makeText(this, "No items to process", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Confirming order", Toast.LENGTH_SHORT).show()
                confirmOrder()
            }
        }

        binding.cancelButton.setOnClickListener {
            Toast.makeText(this, "Cancelling order", Toast.LENGTH_SHORT).show()
            cancelOrder()
        }
    }

    private fun loadSellerOrders() {
        val currentUser = auth.currentUser ?: run {
            Toast.makeText(this, "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        orders.clear()  // Clear the previous orders before fetching new ones

        // Fetch orders made by buyers where the product was sold by the current seller
        db.collection("carts")
            .whereEqualTo("userName", currentUser.displayName)  // Use seller's username to fetch orders
            .whereEqualTo("statusOrder", "Menunggu Konfirmasi Pembelian Anda") // Only get unconfirmed orders
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    Toast.makeText(this, "No orders found", Toast.LENGTH_SHORT).show()
                    adapter.notifyDataSetChanged()
                    return@addOnSuccessListener
                }

                itemCount = 0
                totalPrice = 0.0

                for (doc in documents) {
                    val order = doc.toObject(Order::class.java).apply {
                        docId = doc.id
                        email = doc.getString("email") ?: "Email not available"
                        userName = doc.getString("userName") ?: "Name not available"
                        orderNumber = doc.getString("orderNumber") ?: ""
                        orderDate = doc.getString("orderDate") ?: ""
                        orderTime = doc.getString("orderTime") ?: ""
                        statusOrder = doc.getString("statusOrder") ?: "Ordered"
                        pricePerUnit = doc.getDouble("pricePerUnit") ?: 0.0
                        productName = doc.getString("productName") ?: ""
                        productType = doc.getString("productType") ?: ""
                        quantity = doc.getLong("quantity")?.toInt() ?: 0
                        totalPrice = doc.getDouble("totalPrice") ?: 0.0
                        timestamp = doc.getTimestamp("timestamp")

                        // Fetch image URLs and base64 lists
                        imageUrls = (doc.get("imageUrls") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                        imageBase64List = (doc.get("imageBase64List") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    }

                    orders.add(order)
                    itemCount += order.quantity
                    totalPrice += order.totalPrice
                }

                // Notify the adapter that data has been loaded
                adapter.notifyDataSetChanged()
                updateBottomLayout(itemCount, totalPrice)

                // Check and update button visibility after loading items
                updateButtonVisibility()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load orders: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun confirmOrder() {
        orders.forEach { order ->
            if (order.statusOrder == "Menunggu Konfirmasi Pembelian Anda") {
                order.statusOrder = "Proses Pesanan"
                updateOrderStatus(order)
            }
        }

        binding.confirmButton.visibility = View.GONE
        binding.cancelButton.visibility = View.VISIBLE
        adapter.notifyDataSetChanged()
        updateButtonVisibility()
        updateBottomLayout(itemCount, totalPrice)
    }

    // Fungsi untuk memperbarui status pesanan di Firestore
    private fun updateOrderStatus(order: Order) {
        val currentUser = auth.currentUser ?: return
        if (order.docId.isEmpty()) {
            Toast.makeText(this, "Order ID not found", Toast.LENGTH_SHORT).show()
            return
        }

        val docRef = db.collection("carts")
            .document(currentUser.uid)
            .collection("items")
            .document(order.docId)

        docRef.update("statusOrder", order.statusOrder)
            .addOnSuccessListener {
                Log.d("FirestoreUpdate", "Order successfully updated with status: ${order.statusOrder}")
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to update order", Toast.LENGTH_SHORT).show()
            }
    }

    private fun cancelOrder() {
        // Cancel the order and update status
        orders.forEach { order ->
            order.statusOrder = "Batalkan Proses Pesanan"
            // Update the order status in Firestore
            updateOrderStatus(order)
        }

        // Hide both buttons after cancellation
        binding.confirmButton.visibility = View.GONE
        binding.cancelButton.visibility = View.GONE

        // Reload cart items after cancellation
        loadSellerOrders()

        // Update UI accordingly
        adapter.notifyDataSetChanged()
        updateButtonVisibility()
        updateBottomLayout(itemCount, totalPrice)
    }

    private fun updateBottomLayout(itemCount: Int, totalPrice: Double) {
        binding.itemCount.text = "Items: $itemCount"
        binding.totalPrice.text = "Rp ${String.format("%,.0f", totalPrice)}"
    }

    private fun updateButtonVisibility() {
        // Check if all orders are confirmed or canceled
        val allConfirmedOrCancelled = orders.all {
            it.statusOrder == "Proses Pesanan" || it.statusOrder == "Batalkan Proses Pesanan"
        }

        if (allConfirmedOrCancelled) {
            binding.confirmButton.visibility = View.GONE  // Hide the confirm button if all orders are processed
        } else {
            binding.confirmButton.visibility = View.VISIBLE
            binding.cancelButton.visibility = View.VISIBLE
        }
    }

    private fun deleteOrder(order: Order) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show()
            return
        }

        if (order.docId.isEmpty()) {
            Toast.makeText(this, "Invalid order ID", Toast.LENGTH_SHORT).show()
            return
        }

        val docRef = db.collection("carts")
            .document(currentUser.uid)
            .collection("items")
            .document(order.docId)

        docRef.delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Order successfully deleted", Toast.LENGTH_SHORT).show()

                // Remove the order from the local list
                orders.remove(order)
                itemCount -= order.quantity
                totalPrice -= order.totalPrice
                adapter.notifyDataSetChanged()

                // Update the bottom layout
                updateBottomLayout(itemCount, totalPrice)

                // Reload cart items after deletion
                loadSellerOrders()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to delete order: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
