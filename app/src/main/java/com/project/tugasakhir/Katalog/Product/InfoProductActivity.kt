package com.project.tugasakhir.Katalog.Product

import BottomSheetBuyActivity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Chat.pesan.PesanActivity
import com.project.tugasakhir.Data.Message
import com.project.tugasakhir.Data.Product
import com.project.tugasakhir.Data.Review
import com.project.tugasakhir.Katalog.ProductPenjual.ProductBaruActivity
import com.project.tugasakhir.R
import com.project.tugasakhir.Ulasan.ListUlasanActivity
import com.project.tugasakhir.databinding.ActivityInfoProductBinding

class InfoProductActivity : AppCompatActivity(), BottomSheetBuyActivity.OnAddToCartListener {

    private lateinit var binding: ActivityInfoProductBinding
    private val firestore = FirebaseFirestore.getInstance()  // Initialize Firestore instance
    private val auth = FirebaseAuth.getInstance()
    private var product: Product? = null
    private var source: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfoProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get the product from the Intent
        product = intent.getParcelableExtra("product")

        if (product == null) {
            Toast.makeText(this, "Data produk tidak tersedia", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Setup Toolbar with product name as title
        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Set the title of the AppBar to product's name
        supportActionBar?.title = product?.productName ?: "Produk"

        // Handle back navigation on toolbar
        toolbar.setNavigationOnClickListener {
            onBackPressed()  // Perform the back navigation
        }

        // InfoProductActivity: When "Baca Ulasan" is clicked, navigate to ListUlasanActivity
        binding.bacaUlasan.setOnClickListener {
            val intent = Intent(this, ListUlasanActivity::class.java)
            intent.putExtra("PRODUCT_ID", product?.productId)  // Pass productId to ListUlasanActivity
            startActivity(intent)
        }

        source = intent.getStringExtra("source")

        getReviewsAndRating(product!!)
        // Display product data
        displayProductData(product!!)

        // Get like count and check if liked
        getLikesCount(product!!)
        checkIfLiked(product!!)

        binding.btnKirimPesan.setOnClickListener {
            if (source == "seller") {
                openEditProduct(product!!)
            } else {
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

    private fun getReviewsAndRating(product: Product) {
        firestore.collection("ulasan")
            .document(product.productId)  // Use productId as the unique identifier
            .collection("ulasan")
            .get()
            .addOnSuccessListener { result ->
                val reviewCount = result.size()
                var totalRating = 0f
                val userNames = mutableListOf<String>()  // List to store user names who reviewed the product

                // Calculate the total rating by iterating over reviews
                for (document in result) {
                    val review = document.toObject(Review::class.java)
                    totalRating += (review.ratingKomunikasi + review.ratingKualitas) // Sum ratings
                    userNames.add(review.userName)  // Add the reviewer's username to the list
                }

                // Calculate the average rating (average of all reviews)
                if (reviewCount > 0) {
                    product.avgRating = totalRating / (reviewCount * 2)  // Average rating (normalize)
                }

                // Update the UI elements
                binding.likesCount.text = "$reviewCount Ulasan"
                binding.overallRating.text = String.format("%.1f", product.avgRating)

                // Display the usernames who rated the product
                Log.d("InfoProductActivity", "Users who rated this product: $userNames")

                // Set RatingBar value in InfoProductActivity
                binding.ratingBar.rating = product.avgRating  // Set the actual value of RatingBar
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to get reviews: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkIfLiked(product: Product) {
        val currentUserName = auth.currentUser?.displayName ?: return

        firestore.collection("productLikes")
            .document(product.productId) // Use productId as the unique identifier
            .collection("users")
            .document(currentUserName)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            val reviewCount = data?.getIntExtra("REVIEW_COUNT", 0) ?: 0
        }
    }

    private fun displayProductData(p: Product) {
        with(binding) {
            // Display product details
            namaProduct.text = p.productName
            jenisProduk.text = "Jenis Produk: ${p.productType}"
            HargaBarang.text = if (p.pricePerUnit > 0) "Rp ${String.format("%,.0f", p.pricePerUnit)} /Kg" else "Harga belum tersedia"
            stock.text = "${p.stockAvailable} kg"
            deskripsiProduk.text = p.description
            userName.text = " : ${p.userName}"

            // Set address text
            address.text = " : ${p.alamatToko}"

            // Get seller email to fetch location data
            val sellerEmail = p.email // Use the email from the Product object to fetch seller info
            firestore.collection("seller")
                .document(sellerEmail.replace(".", "_"))  // Firestore uses _ instead of .
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val shareLokasi = document.getString("shareLokasi") ?: ""
                        if (shareLokasi.isNotEmpty()) {
                            // Display the location link
                            linkLokasi.text = shareLokasi
                            // Make the location link clickable
                            linkLokasi.setOnClickListener {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(shareLokasi))
                                intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://com.google.android.apps.maps"))
                                startActivity(intent)
                            }
                        } else {
                            linkLokasi.text = "Lokasi tidak tersedia"
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this@InfoProductActivity, "Failed to get location: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            // Display the discount
            val discount = p.discount
            if (discount > 0) {
                binding.discountLabel.visibility = View.VISIBLE
                binding.discountLabel.text = "$discount% OFF"
            } else {
                binding.discountLabel.visibility = View.GONE
            }

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
            checkIfLiked(p)
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
        val currentUserName = auth.currentUser?.displayName ?: "Unknown"

        val productLikesRef = firestore.collection("productLikes")
            .document(product.productId)  // Use productId as the document ID
            .collection("users")
            .document(currentUserName) // Use username as the document ID

        productLikesRef.get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // If the product is already liked by the user, remove like
                    productLikesRef.delete()
                        .addOnSuccessListener {
                            updateLikeButtonStatus(false)
                            updateLikesCount(product, false) // Decrease like count
                            // Remove username from the list of likes
                            removeUsernameFromLikes(product.productId, currentUserName)
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Failed to remove like: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    // If the product is not liked by the user, add like
                    productLikesRef.set(mapOf("likedAt" to FieldValue.serverTimestamp()))
                        .addOnSuccessListener {
                            updateLikeButtonStatus(true)
                            updateLikesCount(product, true) // Increase like count
                            // Add username to the list of likes
                            addUsernameToLikes(product.productId, currentUserName)
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

    private fun addUsernameToLikes(productId: String, username: String) {
        firestore.collection("productLikes")
            .document(productId)
            .collection("users")
            .document(username)  // Use username as the document ID
            .set(mapOf("username" to username, "likedAt" to FieldValue.serverTimestamp()))
            .addOnSuccessListener {
                Log.d("InfoProductActivity", "Username added to likes: $username")
            }
            .addOnFailureListener { e ->
                Log.e("InfoProductActivity", "Failed to add username to likes: ${e.message}")
            }
    }

    private fun removeUsernameFromLikes(productId: String, username: String) {
        firestore.collection("productLikes")
            .document(productId)
            .collection("users")
            .document(username)  // Use username as the document ID
            .delete()
            .addOnSuccessListener {
                Log.d("InfoProductActivity", "Username removed from likes: $username")
            }
            .addOnFailureListener { e ->
                Log.e("InfoProductActivity", "Failed to remove username from likes: ${e.message}")
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
        firestore.collection("productLikes")  // Replaced db with firestore
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
            binding.btnLike.setImageResource(R.drawable.liked)  // Image when liked
        } else {
            binding.btnLike.setImageResource(R.drawable.like)  // Image when not liked
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
