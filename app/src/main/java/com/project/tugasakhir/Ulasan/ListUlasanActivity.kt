package com.project.tugasakhir.Ulasan

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Adapter.UlasanAdapter
import com.project.tugasakhir.Data.Review
import com.project.tugasakhir.databinding.ActivityListUlasanBinding

class ListUlasanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityListUlasanBinding
    private val db = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityListUlasanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get the productId passed from the InfoProductActivity
        val productId = intent.getStringExtra("PRODUCT_ID") ?: ""

        if (productId.isEmpty()) {
            Toast.makeText(this, "Product ID tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Set the title for the reviews page
        supportActionBar?.title = "Ulasan untuk Produk ID: $productId"

        // Set up RecyclerView to display reviews
        binding.rvReviews.layoutManager = LinearLayoutManager(this)

        // Load reviews for the given product
        loadReviews(productId)
    }

    // Function to fetch reviews from Firestore
    private fun loadReviews(productId: String) {
        db.collection("ulasan")  // Base collection for reviews
            .document(productId)  // Document for specific product
            .collection("ulasan")  // Subcollection containing the reviews
            .get()
            .addOnSuccessListener { documents ->
                val reviews = mutableListOf<Review>()
                for (doc in documents) {
                    val review = doc.toObject(Review::class.java)  // Map document to Review object
                    reviews.add(review)
                }

                // Update review count in InfoProductActivity
                val reviewCount = reviews.size
                val intent = Intent()
                intent.putExtra("REVIEW_COUNT", reviewCount)

                // If there are no reviews, show a message
                if (reviews.isEmpty()) {
                    Toast.makeText(this, "Tidak Ada Ulasan Produk", Toast.LENGTH_SHORT).show()
                }

                // Set up the adapter with the reviews
                val adapter = UlasanAdapter(reviews)
                binding.rvReviews.adapter = adapter

                // Send review count back to InfoProductActivity
                setResult(RESULT_OK, intent)
            }
            .addOnFailureListener { e ->
                // Handle any errors while fetching reviews
                Toast.makeText(this, "Failed to load reviews: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}