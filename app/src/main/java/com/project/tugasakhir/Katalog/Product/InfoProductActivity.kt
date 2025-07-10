package com.project.tugasakhir.Katalog.Product

import BottomSheetBuyActivity
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
import com.project.tugasakhir.Ulasan.ListUlasanActivity
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
        // InfoProductActivity: When "Baca Ulasan" is clicked, navigate to ListUlasanActivity
        binding.bacaUlasan.setOnClickListener {
            val intent = Intent(this, ListUlasanActivity::class.java)
            intent.putExtra("PRODUCT_ID", product?.productId)  // Pass productId to ListUlasanActivity
            startActivity(intent)
        }

        source = intent.getStringExtra("source")

        displayProductData(product!!)

        // Get like count and check if liked
        getLikesCount(product!!)
        checkIfLiked(product!!)

        // Handle message button click
        // In your InfoProductActivity's onCreate or displayProductData method:
        binding.btnKirimPesan.setOnClickListener {
            if (source == "seller") {
                // When the "Edit Produk" button is clicked, navigate to ProductBaruActivity
                openEditProduct(product!!)
            } else {
                // Handle sending message to seller for buyers
                val recipientUserId = product?.userName
                if (recipientUserId != null) {
                    sendMessageToSeller(recipientUserId, product!!)
                } else {
                    Toast.makeText(this, "Penjual tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Handle like button click
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
                Toast.makeText(this, "Failed to check like: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun sendMessageToSeller(recipientUserId: String, product: Product) {
        val messageText = "Tanya tentang produk: ${product.productName}"

        // Ambil UID penjual berdasarkan userName penjual di Firestore
        firestore.collection("users")
            .whereEqualTo("nama", product.userName)  // Asumsi product.userName adalah nama penjual
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    // Ambil UID penjual dari dokumen yang ditemukan
                    val sellerDoc = result.documents.first()
                    val sellerUid = sellerDoc.id  // UID penjual ada di ID dokumen Firestore

                    // Membuat objek pesan
                    val message = Message(
                        senderId = auth.currentUser?.uid ?: "",  // UID pengguna yang sedang login
                        senderName = auth.currentUser?.displayName ?: "Unknown",
                        message = messageText,
                        timestamp = System.currentTimeMillis(),
                        receiverId = sellerUid,  // Menggunakan UID penjual yang diambil dari Firestore
                        receiverName = product.userName,  // Nama penjual diambil dari Product
                        chatId = "chat_${auth.currentUser?.uid}_${sellerUid}" // Menambahkan chatId
                    )

                    val chatId =
                        "chat_${auth.currentUser?.uid}_${sellerUid}" // ChatId berdasarkan senderId dan receiverId

                    firestore.collection("chats").document(chatId).get()
                        .addOnSuccessListener { document ->
                            if (!document.exists()) {
                                // Jika chat belum ada, buat dokumen chat baru dan kirim pesan pertama
                                firestore.collection("chats").document(chatId).set(
                                    hashMapOf(
                                        "participants" to listOf(auth.currentUser?.uid, sellerUid),
                                        "timestamp" to System.currentTimeMillis(),
                                        "receiverName" to product.userName // Nama penerima (penjual)
                                    )
                                ).addOnSuccessListener {
                                    // Tambahkan pesan pertama ke subcollection messages
                                    firestore.collection("chats")
                                        .document(chatId)
                                        .collection("messages")
                                        .add(message)  // Menambahkan chatId ke pesan
                                        .addOnSuccessListener {
                                            Log.d("PesanActivity", "Message sent successfully")
                                            val intent = Intent(this, PesanActivity::class.java)
                                            intent.putExtra("chat_id", chatId)
                                            startActivity(intent)
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("PesanActivity", "Failed to send message: $e")
                                        }
                                }
                            } else {
                                // Jika chat sudah ada, langsung tambahkan pesan baru ke subcollection messages
                                firestore.collection("chats")
                                    .document(chatId)
                                    .collection("messages")
                                    .add(message)  // Menambahkan chatId ke pesan
                                    .addOnSuccessListener {
                                        Log.d("PesanActivity", "Message sent successfully")
                                        val intent = Intent(this, PesanActivity::class.java)
                                        intent.putExtra("chat_id", chatId)
                                        startActivity(intent)
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("PesanActivity", "Failed to send message: $e")
                                    }
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("PesanActivity", "Error checking chat existence: $e")
                        }
                } else {
                    Toast.makeText(this, "Penjual tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("PesanActivity", "Error fetching seller UID: $e")
                Toast.makeText(this, "Error fetching seller UID: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun displayProductData(p: Product) {
        with(binding) {
            // Display product details
            namaProduct.text = p.productName
            jenisProduk.text = "Jenis Produk: ${p.productType}"
            HargaBarang.text = if (p.pricePerUnit > 0) "Rp ${String.format("%,.0f",p.pricePerUnit)}" else "Harga belum tersedia"
            stock.text = "${p.stockAvailable} kg"
            deskripsiProduk.text = p.description
            userName.text = "Penjual: ${p.userName}"
            address.text = "Alamat: ${p.alamatToko}"

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
            // Set button text and listeners based on source
            if (source == "seller") {
                // Change button text and set functionality for seller
                binding.btnBuy.text = "Hapus Produk"
                binding.btnKirimPesan.text = "Edit Produk"

                // Button to delete product
                binding.btnBuy.setOnClickListener {
                    hapusProduk(p) // Handle deleting the product
                }

                // Button to edit product
                btnKirimPesan.setOnClickListener {
                    openEditProduct(p) // Open ProductBaruActivity for editing
                }
            } else {
                // Regular buy product functionality for non-seller users
                binding.btnBuy.text = "Beli Produk"
                binding.btnKirimPesan.text = "Kirim Pesan"
                binding.btnBuy.setOnClickListener {
                    val bottomSheet =
                        BottomSheetBuyActivity.newInstance(product!!) // Send the whole product object
                    bottomSheet.setOnAddToCartListener(this@InfoProductActivity)
                    bottomSheet.show(supportFragmentManager, "BottomSheetBuy")
                }
                binding.btnKirimPesan.setOnClickListener {
                    Toast.makeText(
                        this@InfoProductActivity,
                        "Fitur kirim pesan belum diimplementasi",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun openEditProduct(p: Product) {
        val intent = Intent(this, ProductBaruActivity::class.java).apply {
            putExtra("product", p)
        }
        startActivity(intent)
    }

    private fun hapusProduk(p: Product) {
        firestore.collection("products")
            .document(p.productId)
            .delete()
            .addOnSuccessListener {
                // Remove associated likes as well
                firestore.collection("productLikes")
                    .document(p.productId)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Produk berhasil dihapus beserta likes",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(
                            this,
                            "Gagal menghapus likes: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menghapus produk: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
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
                            Toast.makeText(
                                this,
                                "Failed to remove like: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                } else {
                    productLikesRef.set(mapOf("likedAt" to FieldValue.serverTimestamp()))
                        .addOnSuccessListener {
                            updateLikeButtonStatus(true)
                            updateLikesCount(product, true)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Failed to add like: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Failed to check like status: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun updateLikesCount(product: Product, isLikeAdded: Boolean) {
        val productRef = firestore.collection("products").document(product.productId)

        productRef.get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val currentLikesCount = doc.getLong("likesCount")?.toInt() ?: 0
                    val newLikesCount =
                        if (isLikeAdded) currentLikesCount + 1 else currentLikesCount - 1

                    productRef.update("likesCount", newLikesCount)
                        .addOnSuccessListener {
                            binding.likesCount.text = "$newLikesCount Likes"
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Failed to update like count: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to get likes count: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
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
                Toast.makeText(this, "Failed to get like count: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
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
                Toast.makeText(
                    this,
                    "Failed to update like count: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
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
                        docRef.update(
                            "quantity",
                            newQuantity,
                            "totalPrice",
                            p.pricePerUnit * newQuantity
                        )
                            .addOnSuccessListener {
                                Toast.makeText(this, "Jumlah produk diperbarui", Toast.LENGTH_SHORT)
                                    .show()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this,
                                    "Gagal memperbarui keranjang: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memeriksa keranjang: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
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
                Toast.makeText(this, "Produk berhasil ditambahkan ke keranjang", Toast.LENGTH_SHORT)
                    .show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Gagal menambahkan ke keranjang: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}

