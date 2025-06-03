package com.project.tugasakhir.Cart

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Adapter.OrderAdapter
import com.project.tugasakhir.Data.Order
import com.project.tugasakhir.databinding.FragmentCartBinding

class CartFragment : Fragment() {

    private var _binding: FragmentCartBinding? = null
    private val binding get() = _binding!!

    private val orders = mutableListOf<Order>()
    private lateinit var adapter: OrderAdapter

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.rvPesan.layoutManager = LinearLayoutManager(requireContext())

        adapter = OrderAdapter(orders) { selectedOrder ->
            openOrderDetail(selectedOrder)
        }
        binding.rvPesan.adapter = adapter

        loadUserOrders()
    }

    private fun loadUserOrders() {
        binding.progressbarSettings.visibility = View.VISIBLE
        val currentUser = auth.currentUser
        if (currentUser == null) {
            binding.progressbarSettings.visibility = View.GONE
            Toast.makeText(requireContext(), "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        db.collection("orders")
            .whereEqualTo("userId", currentUser.uid)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    orders.clear()
                    adapter.notifyDataSetChanged()
                    binding.progressbarSettings.visibility = View.GONE
                    return@addOnSuccessListener
                }

                val tempOrders = mutableListOf<Order>()
                val fetchTasks = mutableListOf<com.google.android.gms.tasks.Task<DocumentSnapshot>>()

                for (doc in documents) {
                    val order = doc.toObject(Order::class.java)

                    val penjualUsername = doc.getString("penjualUsername") ?: ""

                    if (penjualUsername.isEmpty()) {

                        tempOrders.add(order)
                        continue
                    }

                    val penjualTask = db.collection("penjual").document(penjualUsername).get()
                        .addOnSuccessListener { penjualDoc ->
                            if (penjualDoc.exists()) {
                                val penjualName = penjualDoc.getString("username") ?: "Nama Tidak Diketahui"
                                val penjualAddress = penjualDoc.getString("alamatToko") ?: "Alamat Tidak Diketahui"

                                val updatedOrder = order.copy(userName = penjualName, userAddress = penjualAddress)

                                tempOrders.add(updatedOrder)
                            } else {
                                tempOrders.add(order)
                            }
                        }
                        .addOnFailureListener {
                            tempOrders.add(order)
                        }

                    fetchTasks.add(penjualTask)
                }

                com.google.android.gms.tasks.Tasks.whenAllComplete(fetchTasks)
                    .addOnSuccessListener {
                        orders.clear()
                        orders.addAll(tempOrders)
                        adapter.notifyDataSetChanged()
                        binding.progressbarSettings.visibility = View.GONE
                    }
                    .addOnFailureListener {
                        orders.clear()
                        orders.addAll(tempOrders)
                        adapter.notifyDataSetChanged()
                        binding.progressbarSettings.visibility = View.GONE
                    }
            }
            .addOnFailureListener { e ->
                binding.progressbarSettings.visibility = View.GONE
                Toast.makeText(requireContext(), "Gagal memuat data pesanan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }





    private fun openOrderDetail(order: Order) {
        val intent = Intent(requireContext(), KeranjangPesananActivity::class.java)
        intent.putExtra("order_data", order)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
