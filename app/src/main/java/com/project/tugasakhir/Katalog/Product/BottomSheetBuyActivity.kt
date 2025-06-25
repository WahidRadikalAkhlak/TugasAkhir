package com.project.tugasakhir.Katalog.Product

import android.content.Intent
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
import com.project.tugasakhir.Data.Product
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ActivityBottomSheetBuyBinding
import kotlinx.coroutines.CoroutineScope
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class BottomSheetBuyActivity : BottomSheetDialogFragment() {
    private var pricePerKg: Double = 0.0
    private var stockAvailable: Int = 0
    private var productName: String = ""
    private var productType: String = ""
    private var pricePerUnit: Double = 0.0
    private var product: Product? = null
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
        private const val ARG_PRODUCT_NAME = "product_name"
        private const val ARG_PRODUCT_TYPE = "product_type"
        private const val ARG_PRICE_PER_UNIT = "price_per_unit"

        fun newInstance(price: Double, stock: Int, productName: String, productType: String, pricePerUnit: Double): BottomSheetBuyActivity {
            val args = Bundle()
            args.putDouble(ARG_PRICE, price)
            args.putInt(ARG_STOCK, stock)
            args.putString(ARG_PRODUCT_NAME, productName)
            args.putString(ARG_PRODUCT_TYPE, productType)
            args.putDouble(ARG_PRICE_PER_UNIT, pricePerUnit)
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
            productName = it.getString(ARG_PRODUCT_NAME) ?: ""
            productType = it.getString(ARG_PRODUCT_TYPE) ?: ""
            pricePerUnit = it.getDouble(ARG_PRICE_PER_UNIT)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = ActivityBottomSheetBuyBinding.inflate(inflater, container, false)

        // Initialize UI elements
        setupUI()

        // Menyembunyikan "Ajukan Tawaran" dan "LLtawarharga" pada awal
        binding.LLtawarharga.visibility = View.GONE
        binding.btnAjukanTawaran.visibility = View.GONE

        binding.banyakPesanan.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val qty = s.toString().toIntOrNull() ?: 0
                val total = qty * pricePerKg
                binding.totalHarga.text = "Rp ${String.format("%,.0f", total)}"
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Menampilkan "LLtawarharga" dan "Ajukan Tawaran" saat "Tawar Harga" ditekan
        binding.btnTawarHarga.setOnClickListener {
            // Menampilkan "LLtawarharga" dan "Ajukan Tawaran" setelah tombol "Tawar Harga" ditekan
            binding.LLtawarharga.visibility = View.VISIBLE
            binding.btnAjukanTawaran.visibility = View.VISIBLE

            // Menyembunyikan tombol "Tawar Harga" setelah ditekan
            binding.btnTawarHarga.visibility = View.GONE
        }


        binding.hargaTawaran.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val tawaranHarga = s.toString().toDoubleOrNull() ?: 0.0
                val batasTawaran = pricePerKg * 1.2  // Batas tawaran adalah 120% dari harga per Kg

                binding.batasTawaranHarga.text = "Rp ${String.format("%,.0f", batasTawaran)}"

                // Menonaktifkan tombol Ajukan Tawaran jika tawaran harga lebih besar dari batas tawaran
                binding.btnAjukanTawaran.isEnabled = tawaranHarga in 0.0..batasTawaran
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.btnAjukanTawaran.setOnClickListener {
            val tawaranHarga = binding.hargaTawaran.text.toString().toDoubleOrNull() ?: 0.0
            val batasTawaran = pricePerKg * 1.2  // Batas tawaran adalah 120% dari harga per Kg

            if (tawaranHarga in 0.0..batasTawaran) {
                // Handle Ajukan Tawaran (misalnya, simpan tawaran harga di Firestore)
                val db = FirebaseFirestore.getInstance()
                val currentUser = FirebaseAuth.getInstance().currentUser ?: return@setOnClickListener

                val tawaranData = hashMapOf(
                    "userId" to currentUser.uid,
                    "tawaranHarga" to tawaranHarga,
                    "status" to "Menunggu Konfirmasi",
                    "timestamp" to FieldValue.serverTimestamp()
                )

                db.collection("tawaran_harga")
                    .add(tawaranData)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "Tawaran harga berhasil diajukan", Toast.LENGTH_SHORT).show()
                        binding.hargaTawaran.setText("")  // Reset input tawaran harga
                        binding.btnAjukanTawaran.isEnabled = false  // Menonaktifkan tombol setelah tawaran diajukan
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(requireContext(), "Gagal mengajukan tawaran: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                Toast.makeText(requireContext(), "Harga tawaran tidak valid atau melebihi batas tawaran", Toast.LENGTH_SHORT).show()
            }
        }

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
        // Use dynamic product data to display product information
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
            "productName" to productName,
            "productType" to productType,
            "pricePerUnit" to pricePerUnit,
            "userName" to (currentUser.displayName ?: "User")
        )

        val db = FirebaseFirestore.getInstance()

        // Save the order to the 'carts' collection under the user's UID
        db.collection("carts")
            .document(currentUser.uid)
            .collection("items")
            .document(orderNumber)
            .set(orderData)
            .addOnSuccessListener {
                // After adding the order to the cart, decrease the product stock
                updateProductStock(productName, quantity)

                Log.d("BottomSheetBuyActivity", "Order added with ID: $orderNumber")
                Toast.makeText(requireContext(), "Order berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                listener?.onAddToCart(quantity)
                dismiss()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal menambahkan order: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateProductStock(productName: String, orderedQuantity: Int) {
        val db = FirebaseFirestore.getInstance()

        // Ensure safe transaction for stock update
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val productQuery = db.collection("products")
                    .whereEqualTo("productName", productName)
                    .limit(1)
                    .get()
                    .await()

                val productDoc = productQuery.documents.firstOrNull()

                if (productDoc != null) {
                    val productRef = db.collection("products").document(productDoc.id)

                    val currentStock = productDoc.getLong("stockAvailable")?.toInt() ?: 0
                    val newStock = currentStock - orderedQuantity

                    if (newStock < 0) {
                        throw Exception("Stok tidak cukup untuk produk ini")
                    }

                    db.runTransaction { transaction ->
                        transaction.update(productRef, "stockAvailable", newStock)
                    }.await()

                    // After stock is updated, send updated product data
                    withContext(Dispatchers.Main) {
                        updateProductInInfoActivity(orderedQuantity)
                    }
                } else {
                    throw Exception("Produk tidak ditemukan di Firestore")
                }
            } catch (e: Exception) {
                Log.e("BottomSheetBuyActivity", "Gagal memperbarui stok", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Gagal memperbarui stok: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateProductInInfoActivity(quantity: Int) {
        val updatedProduct = product?.copy(stockAvailable = stockAvailable - quantity)

        // Check if fragment is attached before updating UI
        if (isAdded && context != null) {
            val intent = Intent(requireContext(), InfoProductActivity::class.java)
            intent.putExtra("product", updatedProduct)  // Passing updated product
            startActivity(intent)
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
