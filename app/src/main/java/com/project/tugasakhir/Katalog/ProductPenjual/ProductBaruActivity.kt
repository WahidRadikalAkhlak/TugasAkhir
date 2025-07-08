package com.project.tugasakhir.Katalog.ProductPenjual

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
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

        // AutoCompleteTextView for product type
        val jenisProdukArray =
            arrayOf("Padi", "Jagung", "Kedelai", "Umbi-Umbian", "Sayur", "Buah", "Tanaman Obat")
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
        binding.jenisProduk.setText(product.productType) // Populate with the product type
        binding.deskripsi.setText(product.description)
        binding.stokTersedia.setText(product.stockAvailable.toString())
        binding.hargaPerUnit.setText(product.pricePerUnit.toString())
        binding.switchTampilkanproduk.isChecked = product.isAvailable

        // You may want to display the product's image (if any) as well
        // Convert base64 to Uri or handle it in RecyclerView if needed
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

        if (namaProduct.isEmpty() || jenisProduk.isEmpty() || deskripsi.isEmpty() ||
            stokTersediaStr.isEmpty() || hargaPerUnitStr.isEmpty()
        ) {
            Toast.makeText(this, "Harap isi semua kolom!", Toast.LENGTH_SHORT).show()
            hideProgressBar()
            return
        }

        val stokTersedia = stokTersediaStr.toIntOrNull()
        val hargaPerUnit = hargaPerUnitStr.toDoubleOrNull()

        if (stokTersedia == null || hargaPerUnit == null) {
            Toast.makeText(this, "Stok dan harga harus berupa angka yang valid", Toast.LENGTH_SHORT)
                .show()
            hideProgressBar()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val base64Images = mutableListOf<String>()
                if (selectedImages.isNotEmpty()) {
                    // If new images are selected, convert them to base64 and update image list
                    for (uri in selectedImages) {
                        val base64 = uriToBase64(uri)
                        if (base64 != null) base64Images.add(base64)
                    }
                } else {
                    // If no new images, retain the old image list from editingProduct
                    base64Images.addAll(editingProduct?.imageBase64List ?: emptyList())
                }

                val sellerUID =
                    FirebaseAuth.getInstance().currentUser?.uid ?: "" // Get current user's UID

                // Prepare product data without email and username if it's an update
                val productData = hashMapOf(
                    "productName" to namaProduct,
                    "productType" to jenisProduk,
                    "description" to deskripsi,
                    "stockAvailable" to stokTersedia,
                    "pricePerUnit" to hargaPerUnit,
                    "isAvailable" to tampilkanProduk,
                    "imageBase64List" to base64Images,
                    "sellerUID" to sellerUID
                )

                if (editingProduct != null) {
                    // If editing an existing product, update the product document
                    val productId = editingProduct?.productId
                    if (productId != null) {
                        productData["productId"] = productId // Add productId to update the document
                        db.collection("products").document(productId)
                            .update(productData)
                            .await() // Menunggu agar update selesai sebelum melanjutkan
                        withContext(Dispatchers.Main) {
                            hideProgressBar()
                            Toast.makeText(
                                this@ProductBaruActivity,
                                "Produk berhasil diperbarui!",
                                Toast.LENGTH_SHORT
                            ).show()
                            // Optionally reload the updated product list in DaftarProductActivity
                            startActivity(
                                Intent(
                                    this@ProductBaruActivity,
                                    DaftarProductActivity::class.java
                                )
                            ) // Ensure this opens the updated list
                            finish() // Close the current activity
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            hideProgressBar()
                            Toast.makeText(
                                this@ProductBaruActivity,
                                "ID produk tidak ditemukan",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                } else {
                    // If it's a new product, create a new document in Firestore
                    val newProductRef = db.collection("products")
                        .document()  // Firestore generates a unique ID automatically
                    productData["productId"] = newProductRef.id  // Add unique product ID
                    productData["email"] =
                        userEmail  // Only add email and username for new products
                    productData["userName"] =
                        userName  // Only add email and username for new products
                    newProductRef.set(productData).await()  // Save the product data

                    withContext(Dispatchers.Main) {
                        hideProgressBar()
                        Toast.makeText(
                            this@ProductBaruActivity,
                            "Produk berhasil disimpan!",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish() // Close the activity after saving
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
        Log.d("UpdateProduct", "Updating product with email: $userEmail and username: $userName")
    }

    private fun showProgressBar() {
        binding.progressBar.visibility = View.VISIBLE
    }

    private fun hideProgressBar() {
        binding.progressBar.visibility = View.GONE
    }

    private fun uriToBase64(uri: Uri): String? {
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }
}
