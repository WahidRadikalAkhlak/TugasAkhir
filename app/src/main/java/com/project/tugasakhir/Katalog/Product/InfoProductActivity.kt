package com.project.tugasakhir.Katalog.Product

import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Data.Product
import com.project.tugasakhir.Katalog.ProductPenjual.ProductBaruActivity
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ActivityInfoProductBinding

class InfoProductActivity : AppCompatActivity(), BottomSheetBuyActivity.OnAddToCartListener {

    private lateinit var binding: ActivityInfoProductBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var product: Product? = null
    private var source: String? = null  // "catalog" atau "seller"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfoProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        product = intent.getParcelableExtra("product")
        source = intent.getStringExtra("source")

        if (product == null) {
            Toast.makeText(this, "Data produk tidak tersedia", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        displayProductData(product!!)
    }

    private fun displayProductData(p: Product) {
        with(binding) {
            namaProduct.text = p.productName
            jenisProduk.text = p.productType
            HargaBarang.text = if (p.pricePerUnit > 0)
                "Rp ${String.format("%,.0f", p.pricePerUnit)}"
            else
                "Harga belum tersedia"
            stock.text = "Stok: ${p.stockAvailable} kg"
            deskripsiProduk.text = p.description
            userName.text = "Added by: ${p.userName}"

            if (p.imageUrls.isNotEmpty()) {
                Glide.with(this@InfoProductActivity)
                    .load(p.imageUrls[0])
                    .placeholder(R.drawable.image_icon)
                    .error(R.drawable.image_icon)
                    .into(imgProduct)
            } else if (p.imageBase64List.isNotEmpty()) {
                base64ToBitmap(p.imageBase64List[0])?.let {
                    imgProduct.setImageBitmap(it)
                } ?: imgProduct.setImageResource(R.drawable.image_icon)
            } else {
                imgProduct.setImageResource(R.drawable.image_icon)
            }

            val source = intent.getStringExtra("source")
            if (source == "seller") {
                // Dari halaman penjual -> tombol Edit dan Hapus
                btnBuy.text = "Hapus Produk"
                btnKirimPesan.text = "Edit Produk"

                btnBuy.setOnClickListener {
                    hapusProduk(p)
                }
                btnKirimPesan.setOnClickListener {
                    openEditProduct(p)
                }
            } else {
                // Dari katalog -> tombol untuk Beli produk
                btnBuy.text = "Beli Produk"
                btnKirimPesan.text = "Kirim Pesan"

                btnBuy.setOnClickListener {
                    // Panggil BottomSheetBuyActivity untuk pembelian
                    val bottomSheet = BottomSheetBuyActivity.newInstance(p.pricePerUnit, p.stockAvailable)
                    bottomSheet.setOnAddToCartListener(this@InfoProductActivity)
                    bottomSheet.show(supportFragmentManager, "BottomSheetBuy")
                }

                btnKirimPesan.setOnClickListener {
                    Toast.makeText(this@InfoProductActivity, "Fitur kirim pesan belum diimplementasi", Toast.LENGTH_SHORT).show()
                }
            }

            btnLike.setOnClickListener {
                // TODO: implementasi like produk
            }
        }
    }

    private fun openEditProduct(p: Product) {
        val intent = Intent(this, ProductBaruActivity::class.java).apply {
            putExtra("product", p)
        }
        startActivity(intent)
        finish()
    }

    private fun hapusProduk(p: Product) {
        firestore.collection("products")
            .whereEqualTo("productName", p.productName)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (doc in documents) {
                        firestore.collection("products").document(doc.id).delete()
                    }
                    Toast.makeText(this, "Produk berhasil dihapus", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Produk tidak ditemukan untuk dihapus", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menghapus produk: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onAddToCart(quantity: Int) {
        val p = product ?: return
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Harap login terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        // Cek apakah produk sudah ada di keranjang
        firestore.collection("carts").document(userId).collection("items")
            .whereEqualTo("productName", p.productName)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    // Produk belum ada di keranjang, tambahkan ke keranjang
                    addProductToCart(p, quantity, userId)
                } else {
                    // Produk sudah ada, perbarui quantity
                    for (doc in documents) {
                        val docRef = firestore.collection("carts")
                            .document(userId)
                            .collection("items")
                            .document(doc.id)

                        // Update quantity jika produk sudah ada
                        val newQuantity = doc.getLong("quantity")?.toInt() ?: 0 + quantity
                        docRef.update("quantity", newQuantity, "totalPrice", p.pricePerUnit * newQuantity)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Jumlah produk diperbarui", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Gagal memperbarui keranjang: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memeriksa keranjang: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // Fungsi untuk menambahkan produk baru ke keranjang
    private fun addProductToCart(p: Product, quantity: Int, userId: String) {
        val cartItem = hashMapOf(
            "productName" to p.productName,
            "productType" to p.productType,
            "quantity" to quantity,
            "pricePerUnit" to p.pricePerUnit,
            "totalPrice" to p.pricePerUnit * quantity,
            "userName" to p.userName,
            "timestamp" to FieldValue.serverTimestamp()
        )

        firestore.collection("carts").document(userId).collection("items")
            .add(cartItem)
            .addOnSuccessListener {
                Toast.makeText(this, "Produk berhasil ditambahkan ke keranjang", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menambahkan ke keranjang: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = android.util.Base64.decode(base64Str, android.util.Base64.DEFAULT)
            android.graphics.BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
