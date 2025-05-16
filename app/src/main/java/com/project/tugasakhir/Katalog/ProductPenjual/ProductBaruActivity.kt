package com.project.tugasakhir.Katalog.ProductPenjual

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Adapter.ImageAdapter
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
    private lateinit var adapter: ImageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProductBaruBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = ImageAdapter(selectedImages, this)
        binding.RVImagenes.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.RVImagenes.adapter = adapter

        binding.addImgProduct.setOnClickListener { openGallery() }

        binding.BtnSimpanProduk.setOnClickListener {
            if (selectedImages.isEmpty()) {
                Toast.makeText(this, "Silakan tambahkan minimal satu foto produk", Toast.LENGTH_SHORT).show()
            } else {
                saveProductToFirestore()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        getImageResult.launch(intent)
    }

    private val getImageResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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
                adapter.notifyDataSetChanged()
            }
        } else {
            Toast.makeText(this, "Gagal memilih gambar", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uriToBase64(uri: Uri): String? {
        val inputStream = contentResolver.openInputStream(uri) ?: return null
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)  // Kompres jpeg 80%
        val byteArray = outputStream.toByteArray()
        return Base64.encodeToString(byteArray, Base64.DEFAULT)
    }

    private fun saveProductToFirestore() {
        val namaProduct = binding.namaProduct.text.toString().trim()
        val jenisProduk = binding.jenisProduk.text.toString().trim()
        val deskripsi = binding.deskripsi.text.toString().trim()
        val stokTersediaStr = binding.stokTersedia.text.toString().trim()
        val hargaPerUnitStr = binding.hargaPerUnit.text.toString().trim()
        val tampilkanProduk = binding.switchTampilkanproduk.isChecked

        if (namaProduct.isEmpty() || jenisProduk.isEmpty() || deskripsi.isEmpty() ||
            stokTersediaStr.isEmpty() || hargaPerUnitStr.isEmpty()) {
            Toast.makeText(this, "Harap isi semua kolom!", Toast.LENGTH_SHORT).show()
            return
        }

        val stokTersedia = stokTersediaStr.toIntOrNull()
        val hargaPerUnit = hargaPerUnitStr.toDoubleOrNull()

        if (stokTersedia == null || hargaPerUnit == null) {
            Toast.makeText(this, "Stok dan harga harus berupa angka yang valid", Toast.LENGTH_SHORT).show()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val base64Images = mutableListOf<String>()
                for (uri in selectedImages) {
                    val base64 = uriToBase64(uri)
                    if (base64 != null) {
                        base64Images.add(base64)
                    }
                }

                val productData = hashMapOf(
                    "productName" to namaProduct,
                    "productType" to jenisProduk,
                    "description" to deskripsi,
                    "stockAvailable" to stokTersedia,
                    "pricePerUnit" to hargaPerUnit,
                    "isAvailable" to tampilkanProduk,
                    "imageBase64List" to base64Images
                )

                db.collection("products").add(productData).await()

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProductBaruActivity, "Produk berhasil disimpan!", Toast.LENGTH_SHORT).show()
                    selectedImages.clear()
                    adapter.notifyDataSetChanged()
                    finish()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ProductBaruActivity, "Gagal menyimpan produk: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}
