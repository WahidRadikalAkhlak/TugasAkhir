import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.project.tugasakhir.Data.Product
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ActivityBottomSheetBuyBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.NumberFormat
import java.util.Locale
import kotlin.io.encoding.Base64
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
    private val selectedImages = mutableListOf<Uri>()

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
        product?.let { p ->
            loadProductImage(p) // Pastikan product tidak null
        } ?: run {
            binding.imgProduct.setImageResource(R.drawable.image_icon) // Gambar default jika produk null
        }
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

    private fun loadProductImage(product: Product) {
        binding.productName.text = product.productName
        binding.productType.text = product.productType

        // Display the product image
        if (!product.imageUrls.isNullOrEmpty()) {
            // Load image from URL (preferred)
            Glide.with(this@BottomSheetBuyActivity)
                .load(product.imageUrls[0])
                .placeholder(R.drawable.image_icon)
                .error(R.drawable.image_icon)
                .into(binding.imgProduct)
        } else if (!product.imageBase64List.isNullOrEmpty()) {
            base64ToBitmap(product.imageBase64List[0])?.let {
                binding.imgProduct.setImageBitmap(it)
            } ?: binding.imgProduct.setImageResource(R.drawable.image_icon) // Default image if base64 decoding fails
        } else {
            binding.imgProduct.setImageResource(R.drawable.image_icon) // Default image if no image URL or base64
        }
    }

    private fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e("InfoProductActivity", "Failed to decode base64 image", e)
            null
        }
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
            totalPrice >= 200000 -> {
                val discount = product?.discount ?: 0.0 // Get discount from the product
                totalPrice * (1 - discount / 100) // Apply the discount if the total price exceeds 200k
            }
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
            binding.hargaTawaran.text.toString().replace("Rp", "").replace(".", "").replace(",", "").trim()
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

        if (product == null || productId.isEmpty()) {
            Toast.makeText(requireContext(), "Produk tidak valid", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            return
        }

        // Menghitung tawaran harga maksimum
        val maxTawaran = calculateMaxTawaran(quantity)
        // Validasi tawaranHarga
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
        }

        // Tentukan harga akhir (gunakan tawaran harga jika ada, atau harga normal per unit)
        val finalHarga = tawaranHarga ?: (quantity * pricePerKg)

        // Retrieve imageBase64List from Firestore and then proceed to save the order
        getProductImageBase64(productId) { imageBase64List ->
            // If imageBase64List is not empty, use it, otherwise use default images
            val imagesToUse = if (imageBase64List.isNotEmpty()) {
                imageBase64List
            } else {
                listOf("") // If no imageBase64List is found, send an empty string or you can choose a default image
            }

            saveOrderToFirestore(imagesToUse, quantity, finalHarga) // Save the order with image data
        }
    }

    private fun saveOrderToFirestore(imageBase64List: List<String>, quantity: Int, finalHarga: Double) {
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return
        val email = currentUser.email ?: "Alamat tidak tersedia"
        val currentDate = getCurrentDateString()
        val currentTime = getCurrentTimeString()

        // Generate orderNumber as document ID
        val orderNumber = "ORD${System.currentTimeMillis()}${Random.nextInt(1000, 9999)}"

        val orderData = hashMapOf(
            "userId" to currentUser.uid,
            "userName" to (currentUser.displayName ?: "User"),
            "email" to email,
            "statusOrder" to "Memesan",
            "orderDate" to currentDate,
            "orderTime" to currentTime,
            "quantity" to quantity,  // Gunakan Int untuk quantity
            "pricePerKg" to pricePerKg,
            "sellerUID" to product?.sellerUID,
            "totalPrice" to finalHarga, // Gunakan harga akhir dalam Double
            "orderNumber" to orderNumber, // Gunakan orderNumber yang sama untuk Firestore ID
            "timestamp" to FieldValue.serverTimestamp(),
            "productName" to productName,
            "productType" to productType,
            "pricePerUnit" to pricePerUnit,
            "alamatToko" to product?.alamatToko,
            "userName" to (currentUser.displayName ?: "User"),
            "sellerName" to product?.userName,
            "productId" to productId,
            "hargaTawaran" to finalHarga,
            "imageBase64List" to imageBase64List // Menyimpan Base64 gambar dalam array
        )

        val db = FirebaseFirestore.getInstance()

        // Use orderNumber as Firestore document ID
        db.collection("carts")
            .document(orderNumber) // Gunakan orderNumber sebagai ID dokumen
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

    private fun getProductImageBase64(productId: String, onSuccess: (List<String>) -> Unit) {
        val db = FirebaseFirestore.getInstance()
        val productRef = db.collection("products").document(productId)

        productRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val imageBase64List = document.get("imageBase64List") as? List<String> ?: emptyList()
                    onSuccess(imageBase64List) // Return the list of imageBase64
                } else {
                    Log.e("BottomSheetBuyActivity", "Product not found")
                    Toast.makeText(requireContext(), "Produk tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("BottomSheetBuyActivity", "Error getting product: ${e.message}")
                Toast.makeText(requireContext(), "Error getting product: ${e.message}", Toast.LENGTH_SHORT).show()
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
