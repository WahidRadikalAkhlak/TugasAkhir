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
import com.project.tugasakhir.databinding.ActivityBottomSheetBuyBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale
import kotlin.random.Random

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
            product = it.getParcelable(ARG_PRODUCT)
            product?.let {
                pricePerKg = it.pricePerUnit
                stockAvailable = it.stockAvailable
                productName = it.productName
                productType = it.productType
                pricePerUnit = it.pricePerUnit
                productId = it.productId
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = ActivityBottomSheetBuyBinding.inflate(inflater, container, false)

        setupUI()
        setupHargaTawaran()
        binding.progressBar.visibility = View.GONE

        binding.banyakPesanan.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val qty = s.toString().toIntOrNull() ?: 0
                if (qty <= 0) {
                    binding.totalHarga.text = "Rp 0"
                    return
                }

                val total = qty * pricePerKg
                binding.totalHarga.text = "Rp ${String.format("%,.0f", total)}"

                val maxTawaran = calculateMaxTawaran(qty)
                binding.batasTawaranHarga.text = "Rp ${String.format("%,.0f", maxTawaran)}"
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.tambah.setOnClickListener {
            updateQuantity(true)
        }

        binding.kurang.setOnClickListener {
            updateQuantity(false)
        }

        binding.btnKeranjang.setOnClickListener {
            handleAddToCart()
        }

        return binding.root
    }

    private fun setupUI() {
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

    private fun calculateMaxTawaran(qty: Int): Double {
        val totalPrice = qty * pricePerKg
        return when {
            qty in 8..15 -> totalPrice * 0.95  // Diskon 5%
            qty in 15..30 -> totalPrice * 0.90  // Diskon 10%
            qty > 30 -> totalPrice * 0.85      // Diskon 15%
            else -> totalPrice
        }
    }

    private fun formatCurrency(value: Double): String {
        val locale = Locale("id", "ID") // Indonesia locale for proper formatting
        val format = NumberFormat.getCurrencyInstance(locale)
        format.minimumFractionDigits = 0 // Don't show decimal places
        format.maximumFractionDigits = 0 // Don't show decimal places

        return format.format(value) // Return as currency string
    }

    private fun setupHargaTawaran() {
        binding.hargaTawaran.addTextChangedListener(object : TextWatcher {
            var isUserTyping = true // To prevent recursive calls while updating the EditText

            override fun afterTextChanged(s: Editable?) {
                if (isUserTyping) {
                    val priceText = s.toString().replace("Rp", "").replace(".", "").replace(",", "").trim() // Remove "Rp" and commas
                    if (priceText.isNotEmpty()) {
                        try {
                            // Remove any previous TextWatcher temporarily to avoid conflicts during formatting
                            isUserTyping = false

                            // Parse the input price
                            val price = priceText.toDouble()

                            // Format the price back with thousands separator
                            val formattedPrice = formatCurrency(price)

                            // Set the formatted price back to the EditText
                            binding.hargaTawaran.setText(formattedPrice)

                            // Move the cursor to the end after setting the formatted text
                            binding.hargaTawaran.setSelection(formattedPrice.length)

                        } catch (e: NumberFormatException) {
                            Log.e("HargaTawaran", "Error parsing price: ${e.message}")
                        } finally {
                            isUserTyping = true // Re-enable the TextWatcher
                        }
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun handleAddToCart() {
        binding.progressBar.visibility = View.VISIBLE

        val quantity = binding.banyakPesanan.text.toString().toIntOrNull() ?: 0
        val tawaranHargaText =
            binding.hargaTawaran.text.toString().replace("Rp", "").replace(".", "").replace(",", "").trim() // Remove "Rp" and commas
        val tawaranHarga = tawaranHargaText.toDoubleOrNull()

        if (quantity <= 0 || quantity > stockAvailable) {
            Toast.makeText(
                requireContext(),
                "Jumlah pesanan tidak valid atau melebihi stok",
                Toast.LENGTH_SHORT
            ).show()
            binding.progressBar.visibility = View.GONE
            return
        }

        if (productId.isEmpty()) {
            Toast.makeText(requireContext(), "Produk tidak valid", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            return
        }

        val maxTawaran = calculateMaxTawaran(quantity)

        // Debugging: Log the values for comparison
        Log.d("Debug", "Tawaran Harga: $tawaranHarga, Max Tawaran: $maxTawaran")

        if (tawaranHarga != null) {
            if (tawaranHarga < maxTawaran) {
                Toast.makeText(
                    requireContext(),
                    "Harga tawaran harus lebih besar dari Rp ${String.format("%,.0f", maxTawaran)}",
                    Toast.LENGTH_SHORT
                ).show()
                binding.progressBar.visibility = View.GONE
                return
            }
        } else {
            Toast.makeText(requireContext(), "Harga tawaran tidak valid", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            return
        }

        val finalHarga = tawaranHarga ?: (quantity * pricePerKg)

        getSellerUIDFromProduct(productId) { sellerUID ->
            val orderNumber = "ORD${System.currentTimeMillis()}${Random.nextInt(1000, 9999)}"
            val sellerName = product?.userName ?: "Unknown"

            saveOrderToFirestore(sellerUID, orderNumber, quantity, finalHarga, sellerName)
        }
    }

    private fun saveOrderToFirestore(
        userId: String,
        orderNumber: String,
        quantity: Int,
        tawaranHarga: Double,
        sellerName: String
    ) {
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
            "sellerUID" to userId,
            "totalPrice" to tawaranHarga, // Store as raw number
            "orderNumber" to orderNumber,
            "timestamp" to FieldValue.serverTimestamp(),
            "productName" to productName,
            "productType" to productType,
            "pricePerUnit" to pricePerUnit,
            "userName" to (currentUser.displayName ?: "User"),
            "sellerName" to sellerName,
            "productId" to productId,
            "hargaTawaran" to tawaranHarga.toString() // Store as number in Firestore
        )

        val db = FirebaseFirestore.getInstance()

        db.collection("carts")
            .document(orderNumber)
            .set(orderData)
            .addOnSuccessListener {
                Toast.makeText(
                    requireContext(),
                    "Produk berhasil ditambahkan ke keranjang",
                    Toast.LENGTH_SHORT
                ).show()
                updateProductStock(productId, quantity)
                dismiss()
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Gagal menambahkan produk ke keranjang: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
                binding.progressBar.visibility = View.GONE
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
                        Toast.makeText(requireContext(), "Seller UID not found", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Log.d("Product", "Product not found")
                    Toast.makeText(requireContext(), "Product not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("Product", "Error getting product: ${e.message}")
                Toast.makeText(
                    requireContext(),
                    "Error getting product: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
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
                        if (isAdded) {
                            Toast.makeText(requireContext(), "Stok tidak cukup", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                    return@launch
                }

                productRef.update("stockAvailable", newStock).await()

                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        Toast.makeText(
                            requireContext(),
                            "Stok produk berhasil diperbarui",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("BottomSheetBuyActivity", "Gagal memperbarui stok", e)
                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        Toast.makeText(
                            requireContext(),
                            "Gagal memperbarui stok: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
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
