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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Adapter.KatalogAdapter
import com.project.tugasakhir.Adapter.ProductImageAdapter
import com.project.tugasakhir.Data.Product
import com.project.tugasakhir.Data.Review
import com.project.tugasakhir.Katalog.Product.InfoProductActivity
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.FragmentKatalogBinding

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
    private val MIN_LIKES = 2
    private val fallbackWeight = 0.7f

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

        produkAdapter = ProductImageAdapter(produkList) { product ->
            val intent = Intent(context, InfoProductActivity::class.java).apply {
                putExtra("product", product)
                putExtra("source", "catalog")
            }
            context?.startActivity(intent)
        }

        binding.rvProdukList.apply {
            adapter = produkAdapter
            layoutManager = GridLayoutManager(context, 5, GridLayoutManager.VERTICAL, false)
        }

        rekomendasiAdapter = KatalogAdapter(mutableListOf(), { product ->
            val intent = Intent(context, InfoProductActivity::class.java).apply {
                putExtra("product", product)
                putExtra("source", "catalog")
            }
            context?.startActivity(intent)
        }, true)

        binding.rvRekomendasi.apply {
            adapter = rekomendasiAdapter
            layoutManager = GridLayoutManager(context, 4, GridLayoutManager.VERTICAL, false)
        }

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
                }.filter { product -> product.isAvailable || product.stockAvailable > 0 }

                allProducts.clear()
                allProducts.addAll(productsFromFirestore)

                filteredProducts.clear()
                filteredProducts.addAll(productsFromFirestore)

                produkList.clear()
                produkList.addAll(productsFromFirestore)

                // Log kelompok produk per kategori (opsional)
                val groupedByType = produkList.groupBy { it.productType }
                groupedByType.forEach { (productType, products) ->
                    val productDetails = products.joinToString("\n") { p ->
                        "Product Name: ${p.productName}, Price/Kg: ${p.pricePerUnit}, Stock: ${p.stockAvailable}, Likes: ${p.likesCount}"
                    }
                    Log.d("KatalogFragment", "Products in Category: $productType\n$productDetails")
                }

                // Ambil rating + likesCount (likesCount sebenarnya sudah ikut termapping kalau field ada)
                var pending = produkList.size
                if (pending == 0) {
                    produkAdapter.notifyDataSetChanged()
                    applyCollaborativeFiltering()
                    updateRecommendations()
                    return@addOnSuccessListener
                }

                produkList.forEach { product ->
                    // refresh rating dari koleksi review (opsional—kalau avgRating sdh ada, bisa skip)
                    getReviewsAndRating(product)

                    // refresh likesCount langsung dari field dokumen (aman & sesuai struktur)
                    getLikesCountForProduct(product) { _ ->
                        pending--
                        if (pending == 0) {
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

    private fun getLikesCountForProduct(product: Product, onLikesCountFetched: (Int) -> Unit) {
        db.collection("products")
            .document(product.productId)
            .get()
            .addOnSuccessListener { doc ->
                val likesCount = doc.getLong("likesCount")?.toInt() ?: 0
                product.likesCount = likesCount
                onLikesCountFetched(likesCount)
            }
            .addOnFailureListener { e ->
                Log.e("KatalogFragment", "Failed to fetch likesCount: ${e.message}")
                onLikesCountFetched(0)
            }
    }

    private fun getReviewsAndRating(product: Product) {
        db.collection("review")
            .document(product.productId)
            .collection("review")
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
                // biarkan adapter di-notify saat batch selesai
            }
            .addOnFailureListener { e ->
                Log.e("KatalogFragment", "Failed to get reviews: ${e.message}")
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
                val cheapestProduct = productsInCategory.minByOrNull { it.pricePerUnit }
                if (cheapestProduct != null) {
                    recommendedProducts.add(cheapestProduct)
                }
            }
        }

        // If no recommendations, use fallback based on highest ratings
        if (recommendedProducts.isEmpty()) {
            applyFallbackRecommendations()
        } else {
            // Log recommended products with details (price, stock, likes)
            val recommendedProductDetails = recommendedProducts.map { product ->
                "Product Name: ${product.productName}, Price/Kg: ${product.pricePerUnit}, Stock: ${product.stockAvailable}, Likes: ${product.likesCount}, Rating: ${product.avgRating}"
            }

            Log.d("KatalogFragment", "Recommended Products: \n${recommendedProductDetails.joinToString("\n")}")

            rekomendasiAdapter.updateData(recommendedProducts)
        }
    }

    fun calculatePrecision(recommendedProducts: List<Product>, actualLikes: List<Product>): Double {
        if (recommendedProducts.isEmpty()) return 0.0

        val relevantRecommendations = recommendedProducts.filter { recommended ->
            actualLikes.contains(recommended)  // Produk yang benar-benar disukai
        }

        return if (relevantRecommendations.isEmpty()) 0.0
        else relevantRecommendations.size.toDouble() / recommendedProducts.size.toDouble()
    }

    fun calculateMAE(predictedRatings: List<Float>, actualRatings: List<Float>): Double {
        if (predictedRatings.isEmpty() || actualRatings.isEmpty() || predictedRatings.size != actualRatings.size) {
            return 0.0
        }

        val absoluteErrors = predictedRatings.zip(actualRatings) { predicted, actual ->
            Math.abs(predicted - actual)
        }

        return absoluteErrors.average()  // Menghitung rata-rata kesalahan absolut
    }

    fun getActualLikedProducts(): List<Product> {
        return allProducts.filter { it.likesCount >= MIN_LIKES }
    }

    fun getPredictedRatings(products: List<Product>): List<Float> {
        return products.mapNotNull { product ->
            if (product.likesCount > 0 || product.ratings.isNotEmpty()) {
                getPredictedRatingUsingCombinedScore(product, alpha = 0.7f)
            } else {
                null
            }
        }
    }

    fun getPredictedRatingUsingCombinedScore(product: Product, alpha: Float): Float {
        val totalScore = product.ratings.values.sum() + product.likesCount
        val totalCount = product.ratings.size + product.likesCount

        if (totalCount > 0) {
            return totalScore / totalCount
        }
        return 0f
    }

    fun getActualRatings(products: List<Product>): List<Float> {
        return products.map { if (it.likesCount > 0) 1f else 0f }
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

        recommendedProducts.forEach { product ->
            val precision = calculatePrecision(listOf(product), actualLikes)
            totalPrecision += precision

            val predictedRatings = getPredictedRatings(listOf(product))
            val actualRatings = getActualRatings(listOf(product))
            val mae = calculateMAE(predictedRatings, actualRatings)
            totalMAE += mae

            Log.d("KatalogFragment", "Precision for ${product.productName}: $precision")
            Log.d("KatalogFragment", "MAE for ${product.productName}: $mae")
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

        // Filter produk berdasarkan rating atau likes yang cukup
        val filteredProducts = allProducts.filter { it.avgRating >= 4.0 || it.likesCount >= MIN_LIKES }

        Log.d("KatalogFragment", "Filtered Products for Collaborative Filtering: ${filteredProducts.size}")

        if (filteredProducts.isEmpty()) {
            Log.e("KatalogFragment", "No products with sufficient likes or rating")
            applyFallbackRecommendations()
            return
        }

        val productLikesMatrix = mutableListOf<MutableList<Double>>()

        // Menghitung kemiripan antar produk menggunakan cosine similarity
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

        // Menyaring produk berdasarkan similarity (kemiripan)
        val recommendedProducts = recommendProductsBasedOnSimilarity(productLikesMatrix, filteredProducts, topN = 10)

        if (recommendedProducts.isNotEmpty()) {
            rekomendasiAdapter.updateData(recommendedProducts)
            Log.d("KatalogFragment", "Recommended Products: ${recommendedProducts.joinToString(", ") { it.productName }}")
            computeAccuracyMetrics()  // Menghitung akurasi rekomendasi
        } else {
            Log.d("KatalogFragment", "No recommended products found.")
            applyFallbackRecommendations(filteredProducts)
        }
    }

    private fun applyFallbackRecommendations(filteredSource: List<Product> = allProducts) {
        val recommendedProducts = filteredSource
            .sortedByDescending { it.avgRating }  // Fallback berdasarkan rating tertinggi
            .take(10)

        // Jika fallback berdasarkan harga, pilih produk dengan harga terendah
        if (recommendedProducts.isEmpty()) {
            val fallbackByPrice = filteredSource.sortedBy { it.pricePerUnit }.take(10)
            rekomendasiAdapter.updateData(fallbackByPrice)
            Log.d("KatalogFragment", "Fallback based on price: ${fallbackByPrice.joinToString(", ") { it.productName }}")
        } else {
            rekomendasiAdapter.updateData(recommendedProducts)
            Log.d("KatalogFragment", "Fallback Recommendations: ${recommendedProducts.joinToString(", ") { it.productName }}")
        }
    }

    private fun setupChipFilter() {
        binding.chipgrp2.setOnCheckedChangeListener { _, checkedId ->
            selectedChipType = when (checkedId) {
                R.id.chip2 -> "Padi"
                R.id.chip3 -> "Umbi-Umbian"
                R.id.chip4 -> "Kacang-Kacangan"
                R.id.chip5 -> "Sayur Daun"
                R.id.chip6 -> "Buah"
                R.id.chip7 -> "Tanaman Obat"
                R.id.chip8 -> "Tanaman Hias"
                else -> "" // Semua
            }
            applyCombinedFilters()
        }
    }

    private fun computeCosineSimilarity(productA: Product, productB: Product, alpha: Float): Double {
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

        if (magnitudeA == 0.0 || magnitudeB == 0.0) return 0.0

        val similarity = dotProduct / (Math.sqrt(magnitudeA) * Math.sqrt(magnitudeB))

        // Log similarity untuk setiap produk
        Log.d("KatalogFragment", "Cosine Similarity between ${productA.productName} and ${productB.productName}: $similarity")

        return similarity
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