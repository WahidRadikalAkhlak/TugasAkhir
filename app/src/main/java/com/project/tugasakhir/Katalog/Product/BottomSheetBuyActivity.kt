package com.project.tugasakhir.Katalog.Product

import android.content.res.ColorStateList
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ActivityBottomSheetBuyBinding

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

        binding.btnKeranjang.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.btn_color, null))
        binding.btnKeranjang.setTextColor(resources.getColor(R.color.black, null))
        binding.btnTawarHarga.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.btn_color, null))
        binding.btnTawarHarga.setTextColor(resources.getColor(R.color.black, null))
        binding.tambah.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.btn_color, null))
        binding.kurang.backgroundTintList = ColorStateList.valueOf(resources.getColor(R.color.btn_color, null))
        binding.harga.text = "Rp ${String.format("%,.0f", pricePerKg)}"
        binding.stock.text = "$stockAvailable Kg"
        binding.totalHarga.text = "Rp 0"

        binding.banyakPesanan.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val qty = s.toString().toIntOrNull() ?: 0
                val total = qty * pricePerKg
                binding.totalHarga.text = "Rp ${String.format("%,.0f", total)}"
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.tambah.setOnClickListener {
            val currentQty = binding.banyakPesanan.text.toString().toIntOrNull() ?: 0
            if (currentQty < stockAvailable) {
                val newQty = currentQty + 1
                binding.banyakPesanan.setText(newQty.toString())
                binding.banyakPesanan.setSelection(binding.banyakPesanan.text?.length ?: 0) // letakkan cursor di akhir
            }
        }

        binding.kurang.setOnClickListener {
            val currentQty = binding.banyakPesanan.text.toString().toIntOrNull() ?: 0
            if (currentQty > 0) {
                val newQty = currentQty - 1
                binding.banyakPesanan.setText(newQty.toString())
                binding.banyakPesanan.setSelection(binding.banyakPesanan.text?.length ?: 0)
            }
        }


        binding.btnKeranjang.setOnClickListener {
            val qtyStr = binding.banyakPesanan.text.toString()
            val quantity = qtyStr.toIntOrNull() ?: 0

            if (quantity <= 0) {
                // Beri feedback validasi
                return@setOnClickListener
            }
            if (quantity > stockAvailable) {
                // Beri feedback validasi
                return@setOnClickListener
            }

            val currentUser = FirebaseAuth.getInstance().currentUser
            if (currentUser == null) {
                Toast.makeText(requireContext(), "User belum login", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val orderNumber = generateOrderNumber()
            val currentDate = getCurrentDateString()
            val currentTime = getCurrentTimeString()

            // Idealnya alamat diambil dari profil user Firestore, sekarang placeholder
            val userAddress = "User Address here"

            val orderData = mutableMapOf<String, Any>(
                "orderId" to orderNumber,
                "userId" to currentUser.uid,
                "userName" to (currentUser.displayName ?: "User"),
                "userAddress" to userAddress,
                "status" to "Memesan",
                "orderDate" to currentDate,
                "orderTime" to currentTime,
                "quantity" to quantity,
                "pricePerKg" to pricePerKg
            )


            FirebaseFirestore.getInstance().collection("orders").add(orderData)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "Order berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                    listener?.onAddToCart(quantity)
                    dismiss()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Gagal menambahkan order: ${it.message}", Toast.LENGTH_SHORT).show()
                }
        }

        return binding.root
    }


    fun generateOrderNumber(): String {
        return "ORD" + System.currentTimeMillis().toString()
    }

    fun getCurrentDateString(): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd")
        return sdf.format(java.util.Date())
    }

    fun getCurrentTimeString(): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss")
        return sdf.format(java.util.Date())
    }

    private fun onAddToCartClicked(quantity: Int) {
        listener?.onAddToCart(quantity)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
