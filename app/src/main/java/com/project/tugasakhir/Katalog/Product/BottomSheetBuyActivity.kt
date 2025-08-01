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
    private var discount: Double = 0.0
    private var minimumPriceForDiscount: Double = 0.0  // Dapatkan dari Product
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
                discount = it.discount
                minimumPriceForDiscount = it.minimumPriceForDiscount
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
        product?.let { p -> loadProductImage(p) }

        // Set visibility of discount condition based on presence of discount and minimumPriceForDiscount
        if (discount == 0.0 || minimumPriceForDiscount == 0.0) {
            binding.diskonSyarat.visibility = View.GONE // Hide discount condition if no discount or min price for discount
        } else {
            binding.diskonSyarat.text = "Syarat Diskon: Pembelian lebih dari ${formatCurrency(minimumPriceForDiscount)} dengan diskon ${discount}%"
        }

        binding.progressBar.visibility = View.GONE
        binding.banyakPesanan.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                val qty = s.toString().toIntOrNull() ?: 0
                if (qty <= 0) {
                    binding.totalHarga.text = "Rp 0"
                    binding.hargadiskon.text = "Rp 0"
                    return
                }

                // Hitung harga total tanpa diskon
                val total = qty * pricePerKg
                binding.totalHarga.text = "Rp ${String.format("%,.0f", total)}"

                // Hitung harga setelah diskon jika memenuhi syarat harga minimal diskon
                var finalPriceWithDiscount = total
                if (total >= minimumPriceForDiscount) {
                    finalPriceWithDiscount = total * (1 - (discount / 100)) // Terapkan diskon
                }

                binding.hargadiskon.text = "Rp ${String.format("%,.0f", finalPriceWithDiscount)}"
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        binding.tambah.setOnClickListener { updateQuantity(true) }
        binding.kurang.setOnClickListener { updateQuantity(false) }
        binding.btnKeranjang.setOnClickListener { handleAddToCart() }

        return binding.root
    }

    private fun setupUI() {
        binding.harga.text = "Rp ${String.format("%,.0f", pricePerKg)}"
        binding.stock.text = "$stockAvailable Kg"
        binding.totalHarga.text = "Rp 0"
    }

    private fun formatCurrency(amount: Double): String {
        val locale = Locale("id", "ID")
        val currencyFormat = NumberFormat.getCurrencyInstance(locale)
        currencyFormat.minimumFractionDigits = 0
        currencyFormat.maximumFractionDigits = 0
        return currencyFormat.format(amount)
    }

    private fun loadProductImage(product: Product) {
        binding.productName.text = product.productName
        binding.productType.text = product.productType

        // Display the product image
        if (!product.imageUrls.isNullOrEmpty()) {
            Glide.with(this@BottomSheetBuyActivity)
                .load(product.imageUrls[0])
                .placeholder(R.drawable.image_icon)
                .error(R.drawable.image_icon)
                .into(binding.imgProduct)
        } else if (!product.imageBase64List.isNullOrEmpty()) {
            base64ToBitmap(product.imageBase64List[0])?.let {
                binding.imgProduct.setImageBitmap(it)
            } ?: binding.imgProduct.setImageResource(R.drawable.image_icon)
        } else {
            binding.imgProduct.setImageResource(R.drawable.image_icon)
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

    private fun handleAddToCart() {
        binding.progressBar.visibility = View.VISIBLE

        val quantity = binding.banyakPesanan.text.toString().toIntOrNull() ?: 0

        if (quantity <= 0 || quantity > stockAvailable) {
            Toast.makeText(requireContext(), "Jumlah pesanan tidak valid atau melebihi stok", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            return
        }

        if (product == null || productId.isEmpty()) {
            Toast.makeText(requireContext(), "Produk tidak valid", Toast.LENGTH_SHORT).show()
            binding.progressBar.visibility = View.GONE
            return
        }

        // Hitung harga akhir dengan diskon
        val totalPrice = quantity * pricePerKg
        val finalHarga = totalPrice * (1 - (discount / 100)) // Hitung harga diskon

        // Retrieve imageBase64List from Firestore and then proceed to save the order
        getProductImageBase64(productId) { imageBase64List ->
            val imagesToUse = if (imageBase64List.isNotEmpty()) imageBase64List else listOf("")
            saveOrderToFirestore(imagesToUse, quantity, finalHarga)
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
            "quantity" to quantity,
            "pricePerKg" to pricePerKg,
            "sellerUID" to product?.sellerUID,
            "totalPrice" to finalHarga, // Gunakan harga akhir dengan diskon
            "orderNumber" to orderNumber,
            "timestamp" to FieldValue.serverTimestamp(),
            "productName" to productName,
            "productType" to productType,
            "pricePerUnit" to pricePerUnit,
            "alamatToko" to product?.alamatToko,
            "userName" to (currentUser.displayName ?: "User"),
            "sellerName" to product?.userName,
            "productId" to productId,
            "discount" to discount,
            "imageBase64List" to imageBase64List,
            "minimumPriceForDiscount" to minimumPriceForDiscount
        )

        val db = FirebaseFirestore.getInstance()

        db.collection("carts")
            .document(orderNumber)
            .set(orderData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Produk berhasil ditambahkan ke keranjang", Toast.LENGTH_SHORT).show()
                updateProductStock(productId, quantity)
                dismiss()
                binding.progressBar.visibility = View.GONE
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal menambahkan produk ke keranjang: ${e.message}", Toast.LENGTH_SHORT).show()
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
                    onSuccess(imageBase64List)
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
                            Toast.makeText(requireContext(), "Stok tidak cukup", Toast.LENGTH_SHORT).show()
                        }
                    }
                    return@launch
                }

                productRef.update("stockAvailable", newStock).await()

                withContext(Dispatchers.Main) {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Stok produk berhasil diperbarui", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Log.e("BottomSheetBuyActivity", "Gagal memperbarui stok", e)
                withContext(Dispatchers.Main) {
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