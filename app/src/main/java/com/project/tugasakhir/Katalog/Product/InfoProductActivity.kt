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
import com.google.firebase.firestore.ListenerRegistration
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
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var product: Product? = null
    private var source: String? = null
    private var productListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInfoProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        product = intent.getParcelableExtra("product")
        if (product == null) {
            Toast.makeText(this, "Data produk tidak tersedia", Toast.LENGTH_SHORT).show()
            finish(); return
        }

        val toolbar: MaterialToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)
        supportActionBar?.title = product?.productName ?: "Produk"
        toolbar.setNavigationOnClickListener { onBackPressedDispatcher.onBackPressed() }

        source = intent.getStringExtra("source")

        // Data awal
        displayProductData(product!!)
        getReviewsAndRating(product!!)
        startProductDocListener(product!!.productId) // 🔴 listen dokumen produk (likesCount realtime)
        checkIfLiked(product!!)                       // 🔴 cek like di /products/{pid}/likesUsers/{uid}

        // Aksi tombol
        binding.bacaUlasan.setOnClickListener {
            val i = Intent(this, ListUlasanActivity::class.java)
            i.putExtra("PRODUCT_ID", product?.productId)
            startActivity(i)
        }

        binding.btnKirimPesan.setOnClickListener {
            if (source == "seller") openEditProduct(product!!) else {
                val recipientUserId = product?.userName
                if (recipientUserId != null) {
                    sendMessageToSeller(recipientUserId, product!!)
                } else {
                    Toast.makeText(this, "Penjual tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.btnLike.setOnClickListener { toggleLike(product!!) }
    }

    // ========================= Likes: LISTEN DOKUMEN PRODUK =========================
    private fun startProductDocListener(productId: String) {
        if (productId.isBlank()) return
        productListener?.remove()
        productListener = firestore.collection("products")
            .document(productId)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("InfoProductActivity", "product snapshot error: ${err.message}")
                    return@addSnapshotListener
                }
                if (snap != null && snap.exists()) {
                    val likesCount = snap.getLong("likesCount")?.toInt() ?: 0
                    binding.likesCount.text = "$likesCount Likes"
                }
            }
    }

    // ========================= Likes: CEK SUDAH LIKE ATAU BELUM =========================
    private fun checkIfLiked(product: Product) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("products")
            .document(product.productId)
            .collection("likesUsers")
            .document(uid)
            .get()
            .addOnSuccessListener { doc -> updateLikeButtonStatus(doc.exists()) }
            .addOnFailureListener { e ->
                Log.e("InfoProductActivity", "checkIfLiked fail: ${e.message}")
                updateLikeButtonStatus(false)
            }
    }

    // ========================= Likes: TOGGLE DENGAN TRANSAKSI =========================
    private fun toggleLike(product: Product) {
        val uid = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Harap login terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }
        val pid = product.productId
        val prodRef = firestore.collection("products").document(pid)
        val likeRef = prodRef.collection("likesUsers").document(uid)

        firestore.runTransaction { tx ->
            val prodDoc = tx.get(prodRef)
            val likeDoc = tx.get(likeRef)
            val currLikes = prodDoc.getLong("likesCount") ?: 0L

            if (likeDoc.exists()) {
                // UNLIKE
                tx.delete(likeRef)
                tx.update(prodRef, "likesCount", if (currLikes > 0) currLikes - 1 else 0)
                false
            } else {
                // LIKE
                tx.set(likeRef, mapOf("likedAt" to FieldValue.serverTimestamp()))
                tx.update(prodRef, "likesCount", currLikes + 1)
                true
            }
        }.addOnSuccessListener { likedNow ->
            updateLikeButtonStatus(likedNow)
            // Tidak perlu set text likesCount; listener dokumen akan update otomatis.
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Gagal mengubah like: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    // ========================= Reviews =========================
    private fun getReviewsAndRating(product: Product) {
        val pid = product.productId
        if (pid.isBlank()) {
            binding.overallRating.text = "0.0"
            binding.ratingBar.rating = 0f
            return
        }
        firestore.collection("review").document(pid).collection("review")
            .get()
            .addOnSuccessListener { result ->
                var totalRating = 0f
                val reviewCount = result.size()
                for (document in result) {
                    val review = document.toObject(Review::class.java)
                    totalRating += (review.ratingKomunikasi + review.ratingKualitas)
                }
                val avg = if (reviewCount > 0) totalRating / (reviewCount * 2) else 0f
                product.avgRating = avg
                binding.overallRating.text = String.format("%.1f", avg)
                binding.ratingBar.rating = avg
            }
            .addOnFailureListener {
                binding.overallRating.text = "0.0"
                binding.ratingBar.rating = 0f
            }
    }

    // ========================= UI Produk =========================
    private fun displayProductData(p: Product) = with(binding) {
        namaProduct.text = p.productName
        jenisProduk.text = "Jenis Produk: ${p.productType}"
        HargaBarang.text = if (p.pricePerUnit > 0)
            "Rp ${String.format("%,.0f", p.pricePerUnit)} /Kg" else "Harga belum tersedia"
        stock.text = "${p.stockAvailable} kg"
        deskripsiProduk.text = p.description
        userName.text = " : ${p.userName}"
        address.text = " : ${p.alamatToko}"

        val sellerEmail = p.email
        if (!sellerEmail.isNullOrBlank()) {
            firestore.collection("seller")
                .document(sellerEmail.replace(".", "_"))
                .get()
                .addOnSuccessListener { doc ->
                    val shareLokasi = doc.getString("shareLokasi").orEmpty()
                    linkLokasi.text = if (shareLokasi.isNotEmpty()) shareLokasi else "Lokasi tidak tersedia"
                    if (shareLokasi.isNotEmpty()) {
                        linkLokasi.setOnClickListener {
                            val i = Intent(Intent.ACTION_VIEW, Uri.parse(shareLokasi))
                            i.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://com.google.android.apps.maps"))
                            startActivity(i)
                        }
                    }
                }
                .addOnFailureListener {
                    linkLokasi.text = "Lokasi tidak tersedia"
                }
        } else linkLokasi.text = "Lokasi tidak tersedia"

        val discount = p.discount
        if (discount > 0) {
            discountLabel.visibility = View.VISIBLE
            discountLabel.text = "$discount% OFF"
        } else discountLabel.visibility = View.GONE

        if (!p.imageUrls.isNullOrEmpty()) {
            Glide.with(this@InfoProductActivity)
                .load(p.imageUrls[0])
                .placeholder(R.drawable.image_icon)
                .error(R.drawable.image_icon)
                .into(imgProduct)
        } else if (!p.imageBase64List.isNullOrEmpty()) {
            base64ToBitmap(p.imageBase64List[0])?.let { imgProduct.setImageBitmap(it) }
                ?: imgProduct.setImageResource(R.drawable.image_icon)
        } else imgProduct.setImageResource(R.drawable.image_icon)

        if (source == "seller") {
            btnBuy.text = "Hapus Produk"
            btnKirimPesan.text = "Edit Produk"
            btnBuy.setOnClickListener { hapusProduk(p) }
            btnKirimPesan.setOnClickListener { openEditProduct(p) }
        } else {
            btnBuy.text = "Beli Produk"
            btnKirimPesan.text = "Kirim Pesan"
            btnBuy.setOnClickListener {
                val bottomSheet = BottomSheetBuyActivity.newInstance(product!!)
                bottomSheet.setOnAddToCartListener(this@InfoProductActivity)
                bottomSheet.show(supportFragmentManager, "BottomSheetBuy")
            }
            btnKirimPesan.setOnClickListener {
                Toast.makeText(this@InfoProductActivity, "Fitur kirim pesan belum diimplementasi", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openEditProduct(p: Product) {
        val i = Intent(this, ProductBaruActivity::class.java).apply { putExtra("product", p) }
        startActivity(i)
    }

    private fun hapusProduk(p: Product) {
        // Cukup hapus dokumen produk. (Jika ingin hapus likesUsers, sebaiknya via Cloud Function / batch delete)
        firestore.collection("products").document(p.productId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Produk berhasil dihapus", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal menghapus produk: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateLikeButtonStatus(isLiked: Boolean) {
        binding.btnLike.setImageResource(if (isLiked) R.drawable.liked else R.drawable.like)
    }

    private fun base64ToBitmap(base64Str: String): Bitmap? = try {
        val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
        BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
    } catch (e: Exception) {
        Log.e("InfoProductActivity", "Failed to decode base64 image", e)
        null
    }

    // ========================= Keranjang =========================
    override fun onAddToCart(quantity: Int) {
        val p = product ?: return
        val userId = auth.currentUser?.uid ?: run {
            Toast.makeText(this, "Harap login terlebih dahulu", Toast.LENGTH_SHORT).show()
            return
        }

        firestore.collection("carts").document(userId).collection("items")
            .whereEqualTo("productId", p.productId) // pakai productId supaya unik
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    addProductToCart(p, quantity, userId)
                } else {
                    val doc = documents.first()
                    val docRef = firestore.collection("carts").document(userId).collection("items").document(doc.id)
                    val oldQty = doc.getLong("quantity")?.toInt() ?: 0
                    val newQty = oldQty + quantity
                    docRef.update(
                        mapOf(
                            "quantity" to newQty,
                            "totalPrice" to p.pricePerUnit * newQty
                        )
                    ).addOnSuccessListener {
                        Toast.makeText(this, "Jumlah produk diperbarui", Toast.LENGTH_SHORT).show()
                    }.addOnFailureListener { e ->
                        Toast.makeText(this, "Gagal memperbarui keranjang: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Gagal memeriksa keranjang: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addProductToCart(p: Product, quantity: Int, userId: String) {
        val cartItem = hashMapOf(
            "productId" to p.productId,
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

    override fun onDestroy() {
        productListener?.remove()
        productListener = null
        super.onDestroy()
    }

    // ========================= Chat (tetap) =========================
    private fun sendMessageToSeller(recipientUserId: String, product: Product) {
        val messageText = "Tanya tentang produk: ${product.productName}"
        firestore.collection("users")
            .whereEqualTo("nama", product.userName)
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    val sellerDoc = result.documents.first()
                    val sellerUid = sellerDoc.id
                    val message = Message(
                        senderId = auth.currentUser?.uid ?: "",
                        senderName = auth.currentUser?.displayName ?: "Unknown",
                        message = messageText,
                        timestamp = System.currentTimeMillis(),
                        receiverId = sellerUid,
                        receiverName = product.userName,
                        chatId = "chat_${auth.currentUser?.uid}_${sellerUid}"
                    )
                    val chatId = "chat_${auth.currentUser?.uid}_${sellerUid}"
                    firestore.collection("chats").document(chatId).get()
                        .addOnSuccessListener { document ->
                            if (!document.exists()) {
                                firestore.collection("chats").document(chatId).set(
                                    hashMapOf(
                                        "participants" to listOf(auth.currentUser?.uid, sellerUid),
                                        "timestamp" to System.currentTimeMillis(),
                                        "receiverName" to product.userName
                                    )
                                ).addOnSuccessListener {
                                    firestore.collection("chats").document(chatId)
                                        .collection("messages").add(message)
                                        .addOnSuccessListener {
                                            val intent = Intent(this, PesanActivity::class.java)
                                            intent.putExtra("chat_id", chatId)
                                            startActivity(intent)
                                        }
                                }
                            } else {
                                firestore.collection("chats").document(chatId)
                                    .collection("messages").add(message)
                                    .addOnSuccessListener {
                                        val intent = Intent(this, PesanActivity::class.java)
                                        intent.putExtra("chat_id", chatId)
                                        startActivity(intent)
                                    }
                            }
                        }
                } else {
                    Toast.makeText(this, "Penjual tidak ditemukan", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error fetching seller UID: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == RESULT_OK) {
            val reviewCount = data?.getIntExtra("REVIEW_COUNT", 0) ?: 0
        }
    }

    private fun toggleLikeStatusForUser(product: Product) {
        val currentUserName = auth.currentUser?.displayName ?: "Unknown"

        val productLikesRef = firestore.collection("products")
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
        firestore.collection("products")
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
        firestore.collection("products")
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
        val pid = product.productId
        if (pid.isBlank()) return
        firestore.collection("products").document(pid).collection("users")
            .get()
            .addOnSuccessListener { result ->
                val likesCount = result.size()
                updateLikesCountInProduct(product, likesCount)
            }
            .addOnFailureListener { e ->
                Log.e("InfoProductActivity", "getLikesCount failed: ${e.message}")
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
}
