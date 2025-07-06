package com.project.tugasakhir.Katalog.Product

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
import com.google.firebase.auth.FirebaseUser
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
    private var productId: String = ""
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
        private const val ARG_PRODUCT = "product"
        fun newInstance(product: Product): BottomSheetBuyActivity {
            val args = Bundle()
            args.putParcelable(ARG_PRODUCT, product) // Menyimpan produk lengkap
            val fragment = BottomSheetBuyActivity()
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            product = it.getParcelable(ARG_PRODUCT) // Ambil seluruh objek Product
            product?.let {
                pricePerKg = it.pricePerUnit // Set data dari objek Product
                stockAvailable = it.stockAvailable
                productName = it.productName
                productType = it.productType
                pricePerUnit = it.pricePerUnit
                productId = it.productId
            }
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

                // Diskon berdasarkan jumlah pembelian
                val discount = when {
                    qty in 5..10 -> 0.03 // Diskon 3% untuk 5-10 kg
                    qty in 10..15 -> 0.05 // Diskon 5% untuk 10-15 kg
                    qty in 15..20 -> 0.07 // Diskon 7% untuk 15-20 kg
                    qty > 40 -> 0.10 // Diskon 10% untuk lebih dari 40 kg
                    else -> 0.0 // Tidak ada diskon untuk kurang dari 5 kg
                }

                // Hitung total harga setelah diskon
                val totalAfterDiscount = total * (1 - discount)
                binding.totalHarga.text = "Rp ${String.format("%,.0f", totalAfterDiscount)}"
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

            // Validasi tawaran harga
            if (tawaranHarga in 0.0..batasTawaran) {
                // Menyimpan tawaran harga ke Firestore
                val db = FirebaseFirestore.getInstance()
                val currentUser = FirebaseAuth.getInstance().currentUser ?: return@setOnClickListener

                // Menghitung diskon berdasarkan jumlah pembelian
                val qty = binding.banyakPesanan.text.toString().toIntOrNull() ?: 0
                val discount = when {
                    qty in 5..10 -> 0.05
                    qty in 10..15 -> 0.07
                    qty in 15..40 -> 0.10
                    qty > 40 -> 0.15
                    else -> 0.0
                }

                val totalPrice = qty * pricePerKg * (1 - discount) // Total harga setelah diskon

                val tawaranData = hashMapOf(
                    "userId" to currentUser.uid,
                    "tawaranHarga" to tawaranHarga,
                    "status" to "Menunggu Konfirmasi",  // Status tawaran masih menunggu konfirmasi
                    "timestamp" to FieldValue.serverTimestamp(),
                    "totalPrice" to totalPrice // Menyimpan total harga setelah diskon
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
            Toast.makeText(requireContext(), "Harap login terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        if (productId.isEmpty()) {
            Toast.makeText(requireContext(), "Produk tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        // Get sellerUID and continue processing if successful
        getSellerUIDFromProduct(productId) { sellerUID ->
            val orderNumber = "ORD${System.currentTimeMillis()}${Random.nextInt(1000, 9999)}"
            val sellerName = product?.userName ?: "Unknown"

            // Proceed with saving the order
            saveOrderToFirestore(sellerUID, orderNumber, quantity, productId, sellerName)
        }
    }

    private fun getSellerUIDFromProduct(productId: String, onSuccess: (String) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val productRef = db.collection("products").document(productId)

        productRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val sellerUID = document.getString("sellerUID")
                    if (sellerUID != null) {
                        onSuccess(sellerUID)
                    } else {
                        Log.d("Product", "Seller UID not found in product")
                    }
                } else {
                    Log.d("Product", "Product not found")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Product", "Error getting product: ${e.message}")
            }
    }

    private fun saveOrderToFirestore(sellerUID: String, orderNumber: String, quantity: Int, productId: String, sellerName: String) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val email = currentUser.email ?: "Alamat tidak tersedia"
        val currentDate = getCurrentDateString()
        val currentTime = getCurrentTimeString()

        val orderData = hashMapOf(
            "userId" to currentUser.uid,
            "userName" to (currentUser.displayName ?: "User"),
            "email" to email,
            "statusOrder" to "Memesan",
            "orderDate" to currentDate,
            "orderTime" to currentTime,
            "quantity" to quantity,
            "pricePerKg" to pricePerKg,
            "sellerUID" to sellerUID,  // Seller UID
            "totalPrice" to quantity * pricePerKg,
            "orderNumber" to orderNumber,
            "timestamp" to FieldValue.serverTimestamp(),
            "productName" to productName,
            "productType" to productType,
            "pricePerUnit" to pricePerUnit,
            "userName" to (currentUser.displayName ?: "User"),
            "sellerName" to sellerName,
            "productId" to productId
        )

        val db = FirebaseFirestore.getInstance()

        db.collection("carts")
            .document(orderNumber)  // Menggunakan orderNumber sebagai ID dokumen
            .set(orderData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Order berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                updateProductStock(productId, quantity)
                dismiss()  // Dismiss the bottom sheet after order is placed
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal menambahkan order: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateProductStock(productId: String, orderedQuantity: Int) {
        val db = FirebaseFirestore.getInstance()
        val productRef = db.collection("products").document(productId)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val productDoc = productRef.get().await()
                val currentStock = productDoc.getLong("stockAvailable")?.toInt() ?: 0
                val newStock = currentStock - orderedQuantity

                if (newStock < 0) {
                    withContext(Dispatchers.Main) {
                        // Pastikan fragment masih terhubung ke konteks
                        if (isAdded) {
                            Toast.makeText(requireContext(), "Stok tidak cukup", Toast.LENGTH_SHORT).show()
                        }
                    }
                    return@launch
                }

                productRef.update("stockAvailable", newStock).await()

                withContext(Dispatchers.Main) {
                    // Pastikan fragment masih terhubung ke konteks
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Stok produk berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("BottomSheetBuyActivity", "Gagal memperbarui stok", e)
                withContext(Dispatchers.Main) {
                    // Pastikan fragment masih terhubung ke konteks
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Gagal memperbarui stok: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
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
