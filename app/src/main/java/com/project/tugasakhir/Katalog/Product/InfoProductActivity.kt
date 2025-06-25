package com.project.tugasakhir.Katalog.Product

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Chat.pesan.PesanActivity
import com.project.tugasakhir.Data.Message
import com.project.tugasakhir.Data.Product
import com.project.tugasakhir.Katalog.ProductPenjual.ProductBaruActivity
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ActivityInfoProductBinding

class InfoProductActivity : AppCompatActivity(), BottomSheetBuyActivity.OnAddToCartListener {

    private lateinit var binding: ActivityInfoProductBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var product: Product? = null
    private var source: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfoProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        product = intent.getParcelableExtra("product")

        if (product == null) {
            Toast.makeText(this, "Data produk tidak tersedia", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        displayProductData(product!!)

        // Ambil jumlah like untuk produk dan tampilkan
        getLikesCount(product!!)

        // Cek apakah produk disukai oleh pengguna dan update tombol like
        checkIfLiked(product!!)

        // Kirim pesan ke penjual
        binding.btnKirimPesan.setOnClickListener {
            val recipientUserId = product?.userName
            if (recipientUserId != null) {
                sendMessageToSeller(recipientUserId, product!!)
            } else {
                Toast.makeText(this, "Penjual tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
        }

        // Tombol like diklik
        binding.btnLike.setOnClickListener {
            toggleLikeStatusForUser(product!!)
        }
    }

    private fun checkIfLiked(product: Product) {
        val currentUserId = auth.currentUser?.uid ?: return

        // Cek jika produk sudah disukai oleh pengguna
        firestore.collection("productLikes")
            .document(product.productName)  // Gunakan nama produk sebagai ID dokumen
            .collection("users")
            .document(currentUserId)  // Gunakan userId untuk setiap pengguna
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    updateLikeButtonStatus(true) // Jika sudah di-like, set tombol liked
                } else {
                    updateLikeButtonStatus(false) // Jika belum di-like, set tombol like
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memeriksa like: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendMessageToSeller(recipientUserId: String, product: Product) {
        val messageText = "Tanya tentang produk: ${product.productName}"

        val message = Message(
            senderId = auth.currentUser?.uid ?: "",
            senderName = auth.currentUser?.displayName ?: "Unknown",
            message = messageText,
            timestamp = System.currentTimeMillis()
        )

        val chatId = "chat_${auth.currentUser?.uid}_${recipientUserId}"

        firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener {
                Toast.makeText(this, "Pesan berhasil dikirim", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, PesanActivity::class.java)
                intent.putExtra("chat_id", chatId)
                startActivity(intent)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal mengirim pesan: ${e.message}", Toast.LENGTH_SHORT).show()
            }
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

            // Gambar produk
            if (!p.imageUrls.isNullOrEmpty()) {
                Glide.with(this@InfoProductActivity)
                    .load(p.imageUrls[0])
                    .placeholder(R.drawable.image_icon)
                    .error(R.drawable.image_icon)
                    .into(imgProduct)
            } else if (!p.imageBase64List.isNullOrEmpty()) {
                base64ToBitmap(p.imageBase64List[0])?.let {
                    imgProduct.setImageBitmap(it)
                } ?: imgProduct.setImageResource(R.drawable.image_icon)
            } else {
                imgProduct.setImageResource(R.drawable.image_icon)
            }

            val source = intent.getStringExtra("source")
            if (source == "seller") {
                btnBuy.text = "Hapus Produk"
                btnKirimPesan.text = "Edit Produk"
                btnBuy.setOnClickListener {
                    hapusProduk(p)
                }
                btnKirimPesan.setOnClickListener {
                    openEditProduct(p)
                }
            } else {
                btnBuy.text = "Beli Produk"
                btnKirimPesan.text = "Kirim Pesan"
                btnBuy.setOnClickListener {
                    val bottomSheet = BottomSheetBuyActivity.newInstance(
                        p.pricePerUnit, p.stockAvailable, p.productName, p.productType, p.pricePerUnit
                    )
                    bottomSheet.setOnAddToCartListener(this@InfoProductActivity)
                    bottomSheet.show(supportFragmentManager, "BottomSheetBuy")
                }
                btnKirimPesan.setOnClickListener {
                    Toast.makeText(this@InfoProductActivity, "Fitur kirim pesan belum diimplementasi", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun toggleLikeStatusForUser(product: Product) {
        val currentUserId = auth.currentUser?.uid ?: return
        val currentUserName = auth.currentUser?.displayName ?: "Unknown" // Mendapatkan username pengguna

        firestore.collection("productLikes")
            .document(product.productName)  // Gunakan nama produk sebagai ID dokumen
            .collection("users")
            .document(currentUserName)  // Gunakan username pengguna sebagai ID dokumen
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Jika produk sudah disukai, lakukan un-like
                    firestore.collection("productLikes")
                        .document(product.productName)
                        .collection("users")
                        .document(currentUserName)
                        .delete()
                        .addOnSuccessListener {
                            Toast.makeText(this, "Product Disliked", Toast.LENGTH_SHORT).show()
                            updateLikeButtonStatus(false)
                            getLikesCount(product)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Gagal menghapus like: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // Jika produk belum disukai, lakukan like
                    firestore.collection("productLikes")
                        .document(product.productName)
                        .collection("users")
                        .document(currentUserName)
                        .set(mapOf("likedAt" to FieldValue.serverTimestamp()))
                        .addOnSuccessListener {
                            Toast.makeText(this, "Product Liked", Toast.LENGTH_SHORT).show()
                            updateLikeButtonStatus(true)
                            getLikesCount(product) // Update jumlah like setelah disukai
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Gagal menambahkan like: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memeriksa like: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun updateLikeButtonStatus(isLiked: Boolean) {
        if (isLiked) {
            binding.btnLike.setImageResource(R.drawable.liked)
        } else {
            binding.btnLike.setImageResource(R.drawable.like)
        }
    }
    private fun getLikesCount(product: Product) {
        firestore.collection("productLikes")
            .document(product.productName)
            .collection("users")
            .get()
            .addOnSuccessListener { result ->
                val likesCount = result.size()
                binding.likesCount.text = "$likesCount Likes"
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal mengambil data like: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
    private fun base64ToBitmap(base64Str: String): Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            Log.e("InfoProductActivity", "Failed to decode base64 image", e)
            null
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

        firestore.collection("carts").document(userId).collection("items")
            .whereEqualTo("productName", p.productName)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    addProductToCart(p, quantity, userId)
                } else {
                    for (doc in documents) {
                        val docRef = firestore.collection("carts")
                            .document(userId)
                            .collection("items")
                            .document(doc.id)

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
}

