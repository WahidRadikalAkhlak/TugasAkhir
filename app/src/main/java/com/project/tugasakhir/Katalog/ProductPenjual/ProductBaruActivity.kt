package com.project.tugasakhir.Katalog.ProductPenjual

import BottomSheetBuyActivity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Account.Penjual.DaftarProductActivity
import com.project.tugasakhir.Adapter.ImageAdapter
import com.project.tugasakhir.Data.Product
import com.project.tugasakhir.databinding.ActivityProductBaruBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

import java.text.NumberFormat
import java.util.Locale

class ProductBaruActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProductBaruBinding
    private val selectedImages = mutableListOf<Uri>()
    private val db = FirebaseFirestore.getInstance()
    private lateinit var imageAdapter: ImageAdapter

    private var userEmail: String = ""
    private var userName: String = ""

    private var editingProduct: Product? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductBaruBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupCurrencyFormat()

        // AutoCompleteTextView for product type
        val jenisProdukArray =
            arrayOf(
                "Padi",
                "Umbi-Umbian",
                "Kacang-Kacangan",
                "Sayur Daun",
                "Buah",
                "Tanaman Obat",
                "Tanaman Hias",
            )
        val adapter =
            ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, jenisProdukArray)
        binding.jenisProduk.setAdapter(adapter)

        userEmail = intent.getStringExtra("EMAIL") ?: ""
        userName = intent.getStringExtra("USERNAME") ?: ""

        // Initialize the image adapter for RecyclerView
        imageAdapter = ImageAdapter(selectedImages, this)
        binding.RVImagenes.layoutManager =
            LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.RVImagenes.adapter = imageAdapter

        binding.addImgProduct.setOnClickListener { openGallery() }

        // If editing, populate the form with the existing product data
        editingProduct = intent.getParcelableExtra<Product>("product")
        editingProduct?.let { populateForm(it) }

        binding.BtnSimpanProduk.setOnClickListener {
            if (selectedImages.isEmpty() && editingProduct == null) {
                Toast.makeText(
                    this,
                    "Silakan tambahkan minimal satu foto produk",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                showProgressBar()
                saveOrUpdateProduct()
            }
        }
    }

    private fun populateForm(product: Product) {
        binding.namaProduct.setText(product.productName)
        binding.jenisProduk.setText(product.productType, false)
        binding.deskripsi.setText(product.description)
        binding.discountInput.setText(product.discount.toString()) // <- tanpa ?.

        val minPrice = product.minimumPriceForDiscount
        binding.minimumPriceForDiscountInput.setText(if (minPrice > 0) formatCurrency(minPrice) else "")

        val price = product.pricePerUnit
        binding.hargaPerUnit.setText(if (price > 0) formatCurrency(price) else "")

        binding.switchTampilkanproduk.isChecked = product.isAvailable

        updateImageAdapterFromLists(
            base64List = product.imageBase64List,
            urlList    = product.imageUrls
        )

        if (product.productId.isNotBlank()) {       // <- tanpa ?.
            fetchImagesFromFirestore(product.productId)
        }
    }

    private fun updateImageAdapterFromLists(
        base64List: List<String>,
        urlList: List<String>
    ) {
        selectedImages.clear()

        // Base64 -> Uri sementara (content://) via FileProvider
        base64List.forEach { b64 ->
            base64ToTempUri(b64)?.let { selectedImages.add(it) }
        }

        // URL -> Uri (jika adapter/glide Anda bisa load dari Uri http/https)
        urlList.forEach { url ->
            if (url.isNotBlank()) selectedImages.add(Uri.parse(url))
        }

        imageAdapter.notifyDataSetChanged()
    }

    private fun base64ToTempUri(base64: String): Uri? {
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)

            // bikin file sementara di cache/ (AMAN utk FileProvider dengan <cache-path>)
            val file = java.io.File.createTempFile("prod_", ".jpg", cacheDir)
            file.outputStream().use { it.write(bytes) }

            // pastikan authority sesuai Manifest
            androidx.core.content.FileProvider.getUriForFile(
                this,
                "${packageName}.fileprovider",
                file
            )
        } catch (e: Exception) {
            Log.e("ProductBaruActivity", "base64ToTempUri error", e)
            null
        }
    }

    private fun fetchImagesFromFirestore(productId: String) {
        showProgressBar()
        db.collection("products").document(productId).get()
            .addOnSuccessListener { doc ->
                if (doc != null && doc.exists()) {
                    val base64List = (doc.get("imageBase64List") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val urlList    = (doc.get("imageUrls") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    updateImageAdapterFromLists(base64List, urlList)
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memuat gambar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            .addOnCompleteListener { hideProgressBar() }
    }

    private fun setupCurrencyFormat() {
        // Flag to prevent recursive calls
        var isUserTyping = true

        // Format untuk Harga per satuan kg
        binding.hargaPerUnit.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (isUserTyping) {
                    val input = s.toString().replace("[Rp,.]".toRegex(), "")
                    if (input.isNotEmpty()) {
                        isUserTyping = false  // Disable the TextWatcher during manual changes
                        val formatted = formatCurrency(input.toDouble())
                        binding.hargaPerUnit.setText(formatted)
                        binding.hargaPerUnit.setSelection(formatted.length)
                        isUserTyping = true  // Re-enable the TextWatcher after formatting
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Format untuk Harga Minimal Diskon
        binding.minimumPriceForDiscountInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (isUserTyping) {
                    val input = s.toString().replace("[Rp,.]".toRegex(), "")
                    if (input.isNotEmpty()) {
                        isUserTyping = false
                        val formatted = formatCurrency(input.toDouble())
                        binding.minimumPriceForDiscountInput.setText(formatted)
                        binding.minimumPriceForDiscountInput.setSelection(formatted.length)
                        isUserTyping = true
                    }
                }
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    // Fungsi untuk memformat angka menjadi format mata uang Indonesia (Rupiah)
    private fun formatCurrency(amount: Double): String {
        val locale = Locale("id", "ID")
        val currencyFormat = NumberFormat.getCurrencyInstance(locale)
        currencyFormat.minimumFractionDigits = 0
        currencyFormat.maximumFractionDigits = 0
        return currencyFormat.format(amount)
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        getImageResult.launch(intent)
    }

    private val getImageResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                if (data != null) {
                    if (data.clipData != null) {
                        val count = data.clipData!!.itemCount
                        for (i in 0 until count) {
                            val imageUri = data.clipData!!.getItemAt(i).uri
                            selectedImages.add(imageUri)
                        }
                    } else {
                        data.data?.let { selectedImages.add(it) }
                    }
                    imageAdapter.notifyDataSetChanged()
                }
            } else {
                Toast.makeText(this, "Gagal memilih gambar", Toast.LENGTH_SHORT).show()
            }
        }

    private fun saveOrUpdateProduct() {
        val namaProduct = binding.namaProduct.text.toString().trim()
        val jenisProduk = binding.jenisProduk.text.toString().trim()
        val deskripsi = binding.deskripsi.text.toString().trim()
        val stokTersediaStr = binding.stokTersedia.text.toString().trim()
        val hargaPerUnitStr = binding.hargaPerUnit.text.toString().trim()
        val tampilkanProduk = binding.switchTampilkanproduk.isChecked
        val minimumPriceForDiscountStr = binding.minimumPriceForDiscountInput.text.toString().trim()

        if (namaProduct.isEmpty() || jenisProduk.isEmpty() || deskripsi.isEmpty() ||
            stokTersediaStr.isEmpty() || hargaPerUnitStr.isEmpty()
        ) {
            Toast.makeText(this, "Harap isi semua kolom!", Toast.LENGTH_SHORT).show()
            hideProgressBar()
            return
        }

        val stokTersedia = stokTersediaStr.toIntOrNull()
        val hargaPerUnit = hargaPerUnitStr.replace("[Rp,.]".toRegex(), "").toDoubleOrNull()

        if (stokTersedia == null || hargaPerUnit == null) {
            Toast.makeText(this, "Stok dan harga harus berupa angka yang valid", Toast.LENGTH_SHORT)
                .show()
            hideProgressBar()
            return
        }

        // Mendapatkan nilai diskon dari input (opsional)
        val discountStr = binding.discountInput.text.toString().trim()
        val discount = if (discountStr.isNotEmpty()) discountStr.toIntOrNull() ?: 0 else 0

        // Memastikan minimumPriceForDiscount tidak null dan hanya diberi nilai jika diisi
        val minimumPriceForDiscount = if (minimumPriceForDiscountStr.isNotEmpty()) {
            minimumPriceForDiscountStr.replace("[Rp,.]".toRegex(), "").toDoubleOrNull() ?: 0.0
        } else {
            0.0
        }

        if (discount < 0 || discount > 100) {
            Toast.makeText(this, "Diskon harus antara 0% hingga 100%", Toast.LENGTH_SHORT).show()
            hideProgressBar()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val base64Images = mutableListOf<String>()
                if (selectedImages.isNotEmpty()) {
                    // Jika gambar baru dipilih, konversikan gambar ke base64
                    for (uri in selectedImages) {
                        val base64 = uriToBase64(uri)
                        if (base64 != null) base64Images.add(base64)
                    }
                } else {
                    // Jika tidak ada gambar baru, gunakan gambar lama dari editingProduct
                    base64Images.addAll(editingProduct?.imageBase64List ?: emptyList())
                }

                val sellerUID =
                    FirebaseAuth.getInstance().currentUser?.uid ?: "" // Get current user's UID
                val alamatToko = fetchAlamatTokoFromFirestore()

                // Siapkan data produk
                // Siapkan data produk (FIELD WAJIB + FIELD DUMMY COLAB)
                val productData = hashMapOf(
                    "productName" to namaProduct,
                    "productType" to jenisProduk,            // catatan: di dummy ada "Umbi-umbian", "Kacang-kacangan"
                    "description" to deskripsi,
                    "stockAvailable" to stokTersedia,
                    "pricePerUnit" to hargaPerUnit,
                    "isAvailable" to tampilkanProduk,
                    "imageBase64List" to base64Images,
                    "sellerUID" to sellerUID,
                    "alamatToko" to alamatToko,
                    "discount" to discount,                  // opsional (bukan bagian dummy, tapi tetap disimpan)
                    "minimumPriceForDiscount" to minimumPriceForDiscount
                )

                if (editingProduct != null) {
                    // ===== UPDATE PRODUK =====
                    val productId = editingProduct?.productId
                    if (productId != null) {
                        productData["productId"] = productId  // pastikan tetap ada
                        db.collection("products").document(productId)
                            .update(productData)              // TIDAK menyentuh likes/rating (aman)
                            .await()
                        withContext(Dispatchers.Main) {
                            hideProgressBar()
                            Toast.makeText(this@ProductBaruActivity, "Produk berhasil diperbarui!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this@ProductBaruActivity, DaftarProductActivity::class.java))
                            finish()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            hideProgressBar()
                            Toast.makeText(this@ProductBaruActivity, "ID produk tidak ditemukan", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    // ===== BUAT PRODUK BARU =====
                    val newRef = db.collection("products").document()
                    productData["productId"] = newRef.id
                    productData["email"] = userEmail
                    productData["userName"] = userName

                    // DEFAULT SKEMA DUMMY (sangat penting):
                    productData["likesCount"] = 0
                    productData["likedBy"] = emptyList<String>()
                    productData["ratingsCount"] = 0
                    productData["avgRating"] = 0.0
                    productData["ratedBy"] = emptyList<Map<String, Any>>() // di dummy: [{user, score}, ...]

                    newRef.set(productData).await()

                    withContext(Dispatchers.Main) {
                        hideProgressBar()
                        Toast.makeText(this@ProductBaruActivity, "Produk berhasil disimpan!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideProgressBar()
                    Toast.makeText(
                        this@ProductBaruActivity,
                        "Gagal menyimpan produk: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private suspend fun fetchAlamatTokoFromFirestore(): String {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email
        val emailForFirestore = userEmail?.replace(".", "_") ?: ""

        val documentSnapshot = db.collection("seller").document(emailForFirestore).get().await()
        return documentSnapshot.getString("alamatToko") ?: "Alamat tidak tersedia"
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
    }

    private fun uriToBase64(uri: Uri): String? = try {
        contentResolver.openInputStream(uri)?.use { inStream ->
            val bitmap = BitmapFactory.decodeStream(inStream)
            val resized = Bitmap.createScaledBitmap(bitmap, 800, 800, true)
            val out = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, 80, out)
            Base64.encodeToString(out.toByteArray(), Base64.DEFAULT)
        }
    } catch (e: Exception) {
        Log.e("ProductBaruActivity", "uriToBase64 error: ${e.message}")
        null
    }
}
