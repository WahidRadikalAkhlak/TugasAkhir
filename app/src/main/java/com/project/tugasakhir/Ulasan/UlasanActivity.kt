package com.project.tugasakhir.Ulasan

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ActivityUlasanBinding

class UlasanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUlasanBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUlasanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get product data from intent
        val productName = intent.getStringExtra("PRODUCT_NAME") ?: "Produk tidak ditemukan"
        val productId = intent.getStringExtra("PRODUCT_ID") ?: ""  // Get product ID from intent

        if (productId.isEmpty()) {
            Toast.makeText(this, "Product ID tidak valid", Toast.LENGTH_SHORT).show()
            finish() // Close activity if productId is not valid
            return
        }

        // Display the product name
        binding.tvBerikanUlasan.text = "Berikan Ulasan untuk Produk: $productName"

        // Get the current user's username (or display name)
        val userName = auth.currentUser?.displayName ?: "Unknown User"

        // Send review
        binding.btnKirimUlasan.setOnClickListener {
            val ratingKomunikasi = binding.ratingKomunikasi.rating
            val ratingKualitas = binding.ratingKualitas.rating
            val komentar = binding.etKomentar.text.toString()

            if (ratingKomunikasi == 0f || ratingKualitas == 0f || komentar.isBlank()) {
                Toast.makeText(this, "Silakan beri rating dan komentar", Toast.LENGTH_SHORT).show()
            } else {
                // Save the review to Firestore
                val ulasan = hashMapOf(
                    "ratingKomunikasi" to ratingKomunikasi,
                    "ratingKualitas" to ratingKualitas,
                    "komentar" to komentar,
                    "produkName" to productName,
                    "userName" to userName  // Add the username of the person submitting the review
                )

                // Save the review under the product's subcollection
                db.collection("ulasan")
                    .document(productId)  // Using product ID as the document ID for the product
                    .collection("ulasan")  // Ulasan sub-collection
                    .add(ulasan)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Ulasan berhasil dikirim", Toast.LENGTH_SHORT).show()

                        // Send result back to InfoProductActivity
                        val resultIntent = Intent()
                        resultIntent.putExtra("REVIEW_COUNT", 1) // Send 1 review since this review was just added
                        setResult(RESULT_OK, resultIntent)
                        finish() // Return to previous activity
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Gagal mengirim ulasan: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}
