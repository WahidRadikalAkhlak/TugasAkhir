package com.project.tugasakhir.Katalog

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Adapter.KatalogAdapter
import com.project.tugasakhir.Adapter.ProductImageAdapter
import com.project.tugasakhir.Data.Product
import com.project.tugasakhir.Data.Review
import com.project.tugasakhir.Katalog.Product.InfoProductActivity
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.FragmentKatalogBinding
import java.math.BigInteger

class KatalogFragment : Fragment() {
    private var _binding: FragmentKatalogBinding? = null
    private val binding get() = _binding!!

    private val allProducts = mutableListOf<Product>()
    private val filteredProducts = mutableListOf<Product>()
    private val produkList = mutableListOf<Product>()
    private var selectedChipType: String = ""
    private lateinit var produkAdapter: ProductImageAdapter
    private lateinit var rekomendasiAdapter: KatalogAdapter
    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up the product adapter
        produkAdapter = ProductImageAdapter(produkList) { product ->
            val intent = Intent(context, InfoProductActivity::class.java).apply {
                putExtra("product", product)
                putExtra("source", "catalog")
            }
            context?.startActivity(intent)
        }

        binding.rvProdukList.adapter = produkAdapter
        binding.rvProdukList.layoutManager =
            GridLayoutManager(requireContext(), 2, GridLayoutManager.HORIZONTAL, false)

        // Set up the recommendation adapter
        rekomendasiAdapter = KatalogAdapter(mutableListOf(), { product ->
            val intent = Intent(context, InfoProductActivity::class.java).apply {
                putExtra("product", product)
                putExtra("source", "catalog")
            }
            context?.startActivity(intent)
        }, true)

        binding.rvRekomendasi.adapter = rekomendasiAdapter
        binding.rvRekomendasi.layoutManager =
            GridLayoutManager(requireContext(), 1, GridLayoutManager.HORIZONTAL, false)

        setupSearch()
        setupChipFilter()
        loadProductsFromFirestore()
    }

    private fun loadProductsFromFirestore() {
        db.collection("products")
            .get()
            .addOnSuccessListener { snapshots ->
                val productsFromFirestore = snapshots.map { doc ->
                    doc.toObject(Product::class.java)
                }.filter { product -> product.isAvailable }

                allProducts.clear()
                allProducts.addAll(productsFromFirestore)

                filteredProducts.clear()
                filteredProducts.addAll(productsFromFirestore)

                produkList.clear()
                produkList.addAll(productsFromFirestore)

                val likesCountFetched = mutableListOf<Int>()
                for (product in produkList) {
                    getReviewsAndRating(product)
                    getLikesCountForProduct(product) { likesCount ->
                        product.likesCount = likesCount
                        likesCountFetched.add(likesCount)
                        if (likesCountFetched.size == produkList.size) {
                            produkAdapter.notifyDataSetChanged()
                            applyCollaborativeFiltering()
                            updateRecommendations()
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to fetch products: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getReviewsAndRating(product: Product) {
        db.collection("ulasan")
            .document(product.productId)
            .collection("ulasan")
            .get()
            .addOnSuccessListener { result ->
                var totalRating = 0f
                val reviewCount = result.size()
                for (document in result) {
                    val review = document.toObject(Review::class.java)
                    totalRating += (review.ratingKomunikasi + review.ratingKualitas)
                }

                if (reviewCount > 0) {
                    product.avgRating = totalRating / (reviewCount * 2)
                }

                produkAdapter.notifyDataSetChanged()
                rekomendasiAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to get reviews: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateRecommendations() {
        val recommendedProducts = mutableListOf<Product>()

        val groupedProducts = allProducts.groupBy { it.productType }
        for ((_, productsInCategory) in groupedProducts) {
            val likedProducts = productsInCategory.filter { it.likesCount > 0 }

            if (likedProducts.isNotEmpty()) {
                recommendedProducts.addAll(likedProducts)
            } else {
                // Gunakan kemiripan produk untuk fallback
                val cheapestProduct = productsInCategory.minByOrNull { it.pricePerUnit }
                if (cheapestProduct != null) {
                    recommendedProducts.add(cheapestProduct)
                }
            }
        }

        // Jika tidak ada produk yang direkomendasikan, gunakan fallback dengan rating tertinggi
        if (recommendedProducts.isEmpty()) {
            applyFallbackRecommendations()
        } else {
            rekomendasiAdapter.updateData(recommendedProducts)
        }
    }

    fun calculatePrecision(recommendedProducts: List<Product>, actualLikes: List<Product>): Double {
        if (recommendedProducts.isEmpty()) return 0.0

        val relevantRecommendations = recommendedProducts.filter { recommended ->
            actualLikes.contains(recommended)  // Produk yang benar-benar disukai
        }

        // Jika tidak ada produk yang relevan ditemukan, maka precision adalah 0
        return if (relevantRecommendations.isEmpty()) 0.0
        else relevantRecommendations.size.toDouble() / recommendedProducts.size.toDouble()
    }

    fun calculateMAE(predictedRatings: List<Float>, actualRatings: List<Float>): Double {
        // Pastikan data tidak kosong dan ukuran sama
        if (predictedRatings.isEmpty() || actualRatings.isEmpty() || predictedRatings.size != actualRatings.size) {
            return 0.0  // Jika data kosong atau tidak cocok, kembalikan 0.0
        }

        // Menghitung kesalahan absolut
        val absoluteErrors = predictedRatings.zip(actualRatings) { predicted, actual ->
            Math.abs(predicted - actual)
        }

        return absoluteErrors.average()  // Menghitung rata-rata kesalahan absolut
    }

    fun getActualLikedProducts(): List<Product> {
        return allProducts.filter { it.likesCount > 0 }
    }

    fun getPredictedRatings(products: List<Product>): List<Float> {
        return products.mapNotNull { product ->
            // Pastikan produk memiliki rating atau likes yang valid untuk dihitung
            if (product.likesCount > 0 || product.ratings.isNotEmpty()) {
                getPredictedRatingUsingCombinedScore(product, alpha = 0.7f)
            } else {
                null  // Jika data tidak lengkap, abaikan perhitungan
            }
        }
    }

    fun getPredictedRatingUsingCombinedScore(product: Product, alpha: Float): Float {
        val totalScore = product.ratings.values.sum() + product.likesCount
        val totalCount = product.ratings.size + product.likesCount

        // Jika produk memiliki data valid, lakukan perhitungan prediksi rating
        if (totalCount > 0) {
            return totalScore / totalCount  // Rata-rata gabungan dari rating dan likes
        }
        return 0f  // Jika tidak ada data, kembalikan rating 0
    }

    fun getActualRatings(products: List<Product>): List<Float> {
        return products.map { if (it.likesCount > 0) 1f else 0f }
    }

    fun getCombinedScore(product: Product, userId: String, alpha: Float): Float {
        val likeScore = if (product.likes.contains(userId)) 1f else 0f  // Like bernilai 1 jika disukai, 0 jika tidak
        val ratingScore = product.ratings[userId] ?: 0f  // Rating pengguna (0 jika tidak ada rating)
        // Menggabungkan rating dan like dengan bobot alpha
        return alpha * ratingScore + (1 - alpha) * likeScore
    }

    fun computeAccuracyMetrics() {
        val recommendedProducts = rekomendasiAdapter.getData()
        if (recommendedProducts.isEmpty()) {
            Log.d("KatalogFragment", "No recommended products found.")
            return
        }

        val actualLikes = getActualLikedProducts()
        var totalPrecision = 0.0
        var totalMAE = 0.0

        // Periksa hanya produk dengan rating dan like yang valid
        recommendedProducts.forEach { product ->
            if (product.ratings.isNotEmpty() || product.likesCount > 0) {
                val precision = calculatePrecision(listOf(product), actualLikes)
                totalPrecision += precision

                val predictedRatings = getPredictedRatings(listOf(product))
                val actualRatings = getActualRatings(listOf(product))
                val mae = calculateMAE(predictedRatings, actualRatings)
                totalMAE += mae

                Log.d("KatalogFragment", "Precision for ${product.productName}: $precision")
                Log.d("KatalogFragment", "MAE for ${product.productName}: $mae")
            }
        }

        val averagePrecision = totalPrecision / recommendedProducts.size
        val averageMAE = totalMAE / recommendedProducts.size

        Log.d("KatalogFragment", "Average Precision: $averagePrecision")
        Log.d("KatalogFragment", "Average MAE: $averageMAE")
    }

    private fun applyCollaborativeFiltering() {
        if (allProducts.isEmpty()) {
            Log.e("KatalogFragment", "No products available for collaborative filtering")
            return
        }

        val filteredProducts = allProducts.filter { it.likesCount > 0 || it.avgRating >= 4.0 }

        Log.d("KatalogFragment", "Filtered Products for Collaborative Filtering: ${filteredProducts.size}")

        if (filteredProducts.isEmpty()) {
            Log.e("KatalogFragment", "No products with sufficient likes or rating")
            applyFallbackRecommendations()
            return
        }

        val productLikesMatrix = mutableListOf<MutableList<Double>>()

        for (i in filteredProducts.indices) {
            val ratingsForProduct = mutableListOf<Double>()
            for (j in filteredProducts.indices) {
                if (i != j) {
                    val similarity = computeCosineSimilarity(filteredProducts[i], filteredProducts[j], alpha = 0.7f)
                    ratingsForProduct.add(similarity)
                }
            }
            productLikesMatrix.add(ratingsForProduct)
        }

        val recommendedProducts = recommendProductsBasedOnSimilarity(productLikesMatrix, filteredProducts, topN = 10)

        if (recommendedProducts.isNotEmpty()) {
            rekomendasiAdapter.updateData(recommendedProducts)
            computeAccuracyMetrics()  // Menghitung akurasi rekomendasi
        } else {
            Log.d("KatalogFragment", "No recommended products found.")
            applyFallbackRecommendations()
        }
    }

    private fun applyFallbackRecommendations(filteredSource: List<Product> = allProducts) {
        val recommendedProducts = filteredSource
            .sortedByDescending { it.avgRating }  // Fallback berdasarkan rating tertinggi
            .take(10)
        rekomendasiAdapter.updateData(recommendedProducts)
    }

    private fun computeAccuracyMetricsForFallback(recommendedProducts: List<Product>) {
        val actualLikes = getActualLikedProducts()
        var totalPrecision = 0.0
        var totalMAE = 0.0

        recommendedProducts.forEach { product ->
            val precision = calculatePrecision(listOf(product), actualLikes)
            totalPrecision += precision

            val predictedRatings = getPredictedRatings(listOf(product))
            val actualRatings = getActualRatings(listOf(product))
            val mae = calculateMAE(predictedRatings, actualRatings)
            totalMAE += mae
        }

        val averagePrecision = totalPrecision / recommendedProducts.size
        val averageMAE = totalMAE / recommendedProducts.size

        Log.d("KatalogFragment", "Average Precision for fallback recommendations: $averagePrecision")
        Log.d("KatalogFragment", "Average MAE for fallback recommendations: $averageMAE")
    }

    private fun setupChipFilter() {
        binding.chipgrp2.setOnCheckedChangeListener { _, checkedId ->
            selectedChipType = when (checkedId) {
                R.id.chip2 -> "Padi"
                R.id.chip3 -> "Jagung"
                R.id.chip4 -> "Kedelai"
                R.id.chip5 -> "Umbi-Umbian"
                R.id.chip6 -> "Sayur"
                R.id.chip7 -> "Buah"
                R.id.chip8 -> "Tanaman Obat"
                else -> "" // Semua
            }
            applyCombinedFilters()
        }
    }

    fun computeCosineSimilarity(productA: Product, productB: Product, alpha: Float): Double {
        val commonUsers = getCommonLikes(productA, productB)
        if (commonUsers.isEmpty()) return 0.0

        var dotProduct = 0.0
        var magnitudeA = 0.0
        var magnitudeB = 0.0

        commonUsers.forEach { userId ->
            val predictedScoreA = getPredictedRatingUsingCombinedScore(productA, alpha)
            val predictedScoreB = getPredictedRatingUsingCombinedScore(productB, alpha)
            dotProduct += predictedScoreA * predictedScoreB
            magnitudeA += predictedScoreA * predictedScoreA
            magnitudeB += predictedScoreB * predictedScoreB
        }

        // Jika magnitude A atau B adalah nol, return 0 karena tidak ada kemiripan
        if (magnitudeA == 0.0 || magnitudeB == 0.0) return 0.0

        return dotProduct / (Math.sqrt(magnitudeA) * Math.sqrt(magnitudeB))
    }

    private fun getCommonLikes(productA: Product, productB: Product): List<String> {
        val commonUsers = mutableListOf<String>()
        val usersA = productA.likes.keys
        val usersB = productB.likes.keys

        // Menemukan pengguna yang ada di kedua produk
        usersA.forEach { userId ->
            if (usersB.contains(userId)) {
                commonUsers.add(userId)
            }
        }
        return commonUsers
    }

    private fun recommendProductsBasedOnSimilarity(
        matrix: List<List<Double>>,
        filteredProducts: List<Product>,
        topN: Int
    ): List<Product> {
        val recommendedProducts = mutableListOf<Product>()

        for (i in matrix.indices) {
            val similarities = matrix[i]

            val sortedSimilarities = similarities.withIndex()
                .sortedByDescending { it.value }
                .take(topN)

            for (similarity in sortedSimilarities) {
                if (similarity.value > 0) {
                    val recommendedProduct = filteredProducts.getOrNull(similarity.index)
                    if (recommendedProduct != null && !recommendedProducts.contains(recommendedProduct)) {
                        recommendedProducts.add(recommendedProduct)
                    }
                }
            }
        }

        return recommendedProducts
    }

    private fun getLikesCountForProduct(product: Product, onLikesCountFetched: (Int) -> Unit) {
        db.collection("productLikes")
            .document(product.productId)
            .collection("users")
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Log.e("KatalogFragment", "No likes found for product: ${product.productId}")
                    onLikesCountFetched(0)
                    return@addOnSuccessListener
                }

                val likesCount = result.size()
                product.likesCount = likesCount
                val likesMap = result.associate { doc -> doc.id to 1 }
                product.likes = likesMap
                onLikesCountFetched(likesCount)
            }
            .addOnFailureListener { e ->
                Log.e("KatalogFragment", "Failed to fetch likes count: ${e.message}")
                onLikesCountFetched(0)
            }
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener { editable ->
            val query = editable.toString().trim()
            applyCombinedFilters(query)
        }
    }

    private fun applyCombinedFilters(searchQuery: String = "") {
        val filtered = allProducts.filter {
            (selectedChipType.isEmpty() || it.productType.equals(selectedChipType, ignoreCase = true)) &&
                    (searchQuery.isEmpty() || it.productName.contains(searchQuery, ignoreCase = true))
        }

        produkList.clear()
        produkList.addAll(filtered)
        produkAdapter.notifyDataSetChanged()
        updateRecommendationsForFiltered(filtered)
    }

    private fun updateRecommendationsForFiltered(filteredProducts: List<Product>) {
        val recommendedProducts = filteredProducts.filter { it.likesCount > 0 }

        if (recommendedProducts.isNotEmpty()) {
            rekomendasiAdapter.updateData(recommendedProducts)
            val actualLikes = getActualLikedProducts()
            val precision = calculatePrecision(recommendedProducts, actualLikes)
            val mae = calculateMAE(getPredictedRatings(recommendedProducts), getActualRatings(actualLikes))

            Log.d("KatalogFragment", "Precision: $precision")
            Log.d("KatalogFragment", "MAE: $mae")
        } else {
            applyFallbackRecommendations(filteredProducts)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}