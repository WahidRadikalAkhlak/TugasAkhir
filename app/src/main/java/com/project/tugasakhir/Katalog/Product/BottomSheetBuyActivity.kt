package com.project.tugasakhir.Katalog.Product

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ActivityBottomSheetBuyBinding
import kotlin.random.Random

class BottomSheetBuyActivity : BottomSheetDialogFragment() {
    private var pricePerKg: Double = 0.0
    private var stockAvailable: Int = 0

    private var _binding: ActivityBottomSheetBuyBinding? = null
    private val binding get() = _binding!!

    interface OnAddToCartListener {
        fun onAddToCart(quantity: Int)
    }

    private var listener: OnAddToCartListener? = null

    fun setOnAddToCartListener(listener: OnAddToCartListener) {
        this.listener = listener
    }

    companion object {
        private const val ARG_PRICE = "price_per_kg"
        private const val ARG_STOCK = "stock_available"

        fun newInstance(price: Double, stock: Int): BottomSheetBuyActivity {
            val args = Bundle()
            args.putDouble(ARG_PRICE, price)
            args.putInt(ARG_STOCK, stock)
            val fragment = BottomSheetBuyActivity()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            pricePerKg = it.getDouble(ARG_PRICE)
            stockAvailable = it.getInt(ARG_STOCK)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = ActivityBottomSheetBuyBinding.inflate(inflater, container, false)

        // Initialize UI elements
        setupUI()

        binding.banyakPesanan.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val qty = s.toString().toIntOrNull() ?: 0
                val total = qty * pricePerKg
                binding.totalHarga.text = "Rp ${String.format("%,.0f", total)}"
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Handle quantity increase
        binding.tambah.setOnClickListener {
            updateQuantity(true)
        }

        // Handle quantity decrease
        binding.kurang.setOnClickListener {
            updateQuantity(false)
        }

        // Handle "Add to Cart" button click
        binding.btnKeranjang.setOnClickListener {
            handleAddToCart()
        }

        return binding.root
    }

    private fun setupUI() {
        // Setup button colors
        binding.btnKeranjang.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.btn_color, null))
        binding.btnKeranjang.setTextColor(resources.getColor(R.color.black, null))
        binding.btnTawarHarga.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.btn_color, null))
        binding.btnTawarHarga.setTextColor(resources.getColor(R.color.black, null))
        binding.tambah.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.btn_color, null))
        binding.kurang.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.btn_color, null))
        binding.harga.text = "Rp ${String.format("%,.0f", pricePerKg)}"
        binding.stock.text = "$stockAvailable Kg"
        binding.totalHarga.text = "Rp 0"
    }

    private fun updateQuantity(increase: Boolean) {
        val currentQty = binding.banyakPesanan.text.toString().toIntOrNull() ?: 0
        val newQty = if (increase) currentQty + 1 else currentQty - 1
        if (newQty in 0..stockAvailable) {
            binding.banyakPesanan.setText(newQty.toString())
            binding.banyakPesanan.setSelection(binding.banyakPesanan.text?.length ?: 0)
        } else if (newQty > stockAvailable) {
            Toast.makeText(requireContext(), "Stok tidak cukup", Toast.LENGTH_SHORT).show()
        }
    }

    private fun handleAddToCart() {
        val quantity = binding.banyakPesanan.text.toString().toIntOrNull() ?: 0
        if (quantity <= 0 || quantity > stockAvailable) {
            Toast.makeText(requireContext(), "Jumlah pesanan tidak valid atau melebihi stok", Toast.LENGTH_SHORT).show()
            return
        }
        val currentUser = FirebaseAuth.getInstance().currentUser ?: run {
            Toast.makeText(requireContext(), "User belum login", Toast.LENGTH_SHORT).show()
            return
        }

        val currentDate = getCurrentDateString()
        val currentTime = getCurrentTimeString()
        val email = currentUser.email ?: "Alamat tidak tersedia"

        val orderNumber = "ORD${System.currentTimeMillis()}${Random.nextInt(1000, 9999)}"

        val orderData = hashMapOf(
            "userId" to currentUser.uid,
            "userName" to (currentUser.displayName ?: "User"),
            "email" to email,
            "statusOrder" to "Memesan",
            "orderDate" to currentDate,
            "orderTime" to currentTime,
            "quantity" to quantity,
            "pricePerKg" to pricePerKg,
            "totalPrice" to quantity * pricePerKg,
            "orderNumber" to orderNumber,
            "timestamp" to FieldValue.serverTimestamp(),
            "productName" to "Kol",  // Assuming hardcoded product details for now
            "productType" to "Sayur",  // Assuming hardcoded product details for now
            "pricePerUnit" to 14000,  // Hardcoded price
            "userName" to "abdul"  // Hardcoded user name for now
        )

        val db = FirebaseFirestore.getInstance()

        // Save the order to the 'carts' collection under the user's UID
        db.collection("carts")
            .document(currentUser.uid) // User-specific cart
            .collection("items") // The items collection within the cart
            .document(orderNumber) // Use orderNumber as document ID
            .set(orderData)
            .addOnSuccessListener {
                Log.d("BottomSheetBuyActivity", "Order added with ID: $orderNumber")
                Toast.makeText(requireContext(), "Order berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                listener?.onAddToCart(quantity)
                dismiss()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal menambahkan order: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getCurrentDateString(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd")
        return sdf.format(java.util.Date())
    }

    private fun getCurrentTimeString(): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss")
        return sdf.format(java.util.Date())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
