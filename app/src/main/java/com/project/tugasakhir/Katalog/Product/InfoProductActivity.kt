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

        firestore.collection("productLikes")
            .document(product.productId) // Use productId as the unique identifier
            .collection("users")
            .document(currentUserId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    updateLikeButtonStatus(true) // If the product is liked by the user
                } else {
                    updateLikeButtonStatus(false) // If the product is not liked by the user
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to check like: ${e.message}", Toast.LENGTH_SHORT).show()
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
            // Display product details
            namaProduct.text = p.productName
            jenisProduk.text = p.productType
            HargaBarang.text = if (p.pricePerUnit > 0) "Rp ${String.format("%,.0f", p.pricePerUnit)}" else "Harga belum tersedia"
            stock.text = "Stok: ${p.stockAvailable} kg"
            deskripsiProduk.text = p.description
            userName.text = "Added by: ${p.userName}"

            // Display the product image
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

            getLikesCount(p)

            val source = intent.getStringExtra("source")
            if (source == "seller") {
                btnBuy.text = "Hapus Produk"
                btnKirimPesan.text = "Edit Produk"  // Change the button text to "Edit Produk"

                // Button click listeners for the seller
                btnBuy.setOnClickListener {
                    hapusProduk(p) // Handle deleting the product
                }

                btnKirimPesan.setOnClickListener {
                    // When the "Edit Produk" button is clicked, navigate to ProductBaruActivity
                    openEditProduct(p)
                }
            } else {
                btnBuy.text = "Beli Produk"
                btnKirimPesan.text = "Kirim Pesan"
                btnBuy.setOnClickListener {
                    val bottomSheet = BottomSheetBuyActivity.newInstance(product!!) // Kirim seluruh objek Product
                    bottomSheet.setOnAddToCartListener(this@InfoProductActivity)
                    bottomSheet.show(supportFragmentManager, "BottomSheetBuy")
                }
                btnKirimPesan.setOnClickListener {
                    Toast.makeText(this@InfoProductActivity, "Fitur kirim pesan belum diimplementasi", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openEditProduct(p: Product) {
        val intent = Intent(this, ProductBaruActivity::class.java).apply {
            // Pass the product data for editing
            putExtra("products", p) // Passing the product object to ProductBaruActivity
        }
        // Start ProductBaruActivity for editing
        startActivity(intent)
    }


    private fun hapusProduk(p: Product) {
        firestore.collection("products")
            .document(p.productId)
            .delete()
            .addOnSuccessListener {
                // Hapus juga likes yang terkait dengan produk ini
                firestore.collection("productLikes")
                    .document(p.productId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(this, "Produk berhasil dihapus beserta likes", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Gagal menghapus likes: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menghapus produk: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun toggleLikeStatusForUser(product: Product) {
        val currentUserId = auth.currentUser?.uid ?: return

        val productLikesRef = firestore.collection("productLikes")
            .document(product.productId) // Use productId for managing likes
            .collection("users")
            .document(currentUserId)

        productLikesRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    productLikesRef.delete()
                        .addOnSuccessListener {
                            updateLikeButtonStatus(false)
                            updateLikesCount(product, false)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to remove like: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    productLikesRef.set(mapOf("likedAt" to FieldValue.serverTimestamp()))
                        .addOnSuccessListener {
                            updateLikeButtonStatus(true)
                            updateLikesCount(product, true)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to add like: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to check like status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateLikesCount(product: Product, isLikeAdded: Boolean) {
        val productRef = firestore.collection("products").document(product.productId)

        productRef.get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val currentLikesCount = doc.getLong("likesCount")?.toInt() ?: 0
                    val newLikesCount = if (isLikeAdded) currentLikesCount + 1 else currentLikesCount - 1

                    productRef.update("likesCount", newLikesCount)
                        .addOnSuccessListener {
                            binding.likesCount.text = "$newLikesCount Likes"
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to update like count: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to get likes count: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getLikesCount(product: Product) {
        firestore.collection("productLikes")
            .document(product.productId)
            .collection("users")
            .get()
            .addOnSuccessListener { result ->
                val likesCount = result.size()
                updateLikesCountInProduct(product, likesCount)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to get like count: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateLikesCountInProduct(product: Product, likesCount: Int) {
        firestore.collection("products")
            .document(product.productId)
            .update("likesCount", likesCount)
            .addOnSuccessListener {
                binding.likesCount.text = "$likesCount Likes"
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to update like count: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateLikeButtonStatus(isLiked: Boolean) {
        if (isLiked) {
            binding.btnLike.setImageResource(R.drawable.liked)
        } else {
            binding.btnLike.setImageResource(R.drawable.like)
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

