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
    private lateinit var produkAdapter: ProductImageAdapter // Adapter for product list
    private lateinit var rekomendasiAdapter: KatalogAdapter // Adapter for recommendations

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

        // Adapter for the product list (without likes count)
        produkAdapter = ProductImageAdapter(produkList) { product ->
            val intent = Intent(context, InfoProductActivity::class.java).apply {
                putExtra("product", product)
                putExtra("source", "catalog")
            }
            context?.startActivity(intent)
        }

        // Set the GridLayoutManager for horizontal scrolling with 2 items per row
        binding.rvProdukList.adapter = produkAdapter
        binding.rvProdukList.layoutManager =
            GridLayoutManager(requireContext(), 2, GridLayoutManager.HORIZONTAL, false)

        // Adapter for recommendations (with likes count)
        rekomendasiAdapter = KatalogAdapter(mutableListOf(), { product ->
            val intent = Intent(context, InfoProductActivity::class.java).apply {
                putExtra("product", product)
                putExtra("source", "catalog")
            }
            context?.startActivity(intent)
        }, true) // Display likes count in recommendations

        binding.rvRekomendasi.adapter =
            rekomendasiAdapter // Set rekomendasiAdapter to rvRekomendasi
        binding.rvRekomendasi.layoutManager = GridLayoutManager(
            requireContext(),
            1,
            GridLayoutManager.HORIZONTAL,
            false
        ) // Horizontal Layout

        setupSearch()
        setupChipFilter()
        loadProductsFromFirestore() // Load products from Firestore
    }

    private fun loadProductsFromFirestore() {
        // Ambil data produk dari Firestore
        db.collection("products")
            .get()
            .addOnSuccessListener { snapshots ->
                val productsFromFirestore = snapshots.map { doc ->
                    doc.toObject(Product::class.java)
                }.filter { product -> product.isAvailable }

                // Menambahkan produk ke dalam list
                allProducts.clear()
                allProducts.addAll(productsFromFirestore)

                filteredProducts.clear()
                filteredProducts.addAll(productsFromFirestore)

                produkList.clear()
                produkList.addAll(productsFromFirestore)

                // Ambil jumlah likes untuk setiap produk dan perbarui UI
                val likesCountFetched = mutableListOf<Int>()
                for (product in produkList) {
                    getLikesCountForProduct(product) { likesCount ->
                        product.likesCount = likesCount
                        likesCountFetched.add(likesCount)
                        if (likesCountFetched.size == produkList.size) {
                            // Semua jumlah likes telah diambil, sekarang refresh RecyclerView
                            produkAdapter.notifyDataSetChanged()

                            // Panggil applyCollaborativeFiltering setelah semua data likes dihitung
                            applyCollaborativeFiltering()  // <-- Panggilan fungsi Collaborative Filtering

                            updateRecommendations()  // Update rekomendasi lainnya
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal mengambil data produk: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateRecommendations() {
        // Group products by product type
        val groupedProducts = allProducts.groupBy { it.productType }

        val recommendedProducts = mutableListOf<Product>()

        // Iterate through each product type
        for ((type, productsInCategory) in groupedProducts) {
            // Filter products with at least 1 like
            val likedProducts = productsInCategory.filter { it.likesCount > 0 }

            if (likedProducts.isNotEmpty()) {
                // If there are liked products, display the ones with likes
                recommendedProducts.addAll(likedProducts)
            } else {
                // If no liked products, display the cheapest product in that category
                val cheapestProduct = productsInCategory.minByOrNull { it.pricePerUnit }
                if (cheapestProduct != null) {
                    recommendedProducts.add(cheapestProduct)
                }
            }
        }

        // Update the recommendations adapter with the filtered list
        rekomendasiAdapter.updateData(recommendedProducts)
    }

    private fun applyCollaborativeFiltering() {
        if (allProducts.isEmpty()) {
            Log.e("KatalogFragment", "No products available for collaborative filtering")
            return
        }

        // Mengambil produk dengan likes lebih dari 0 (dapat diubah menjadi lebih besar dari 3 jika perlu)
        val filteredProducts = allProducts.filter { it.likesCount > 0 }

        if (filteredProducts.isEmpty()) {
            Log.e("KatalogFragment", "No products with sufficient likes")
            applyFallbackRecommendations()  // Jika tidak ada produk dengan likes > 0, tampilkan produk termurah
            return
        }

        // Membuat matriks kemiripan produk berdasarkan likes
        val productLikesMatrix = mutableListOf<MutableList<Double>>()

        // Menghitung kemiripan antar produk berdasarkan likes
        for (i in filteredProducts.indices) {
            val likesForProduct = mutableListOf<Double>()
            for (j in filteredProducts.indices) {
                if (i != j) {
                    val similarity = computeCosineSimilarity(filteredProducts[i], filteredProducts[j])
                    likesForProduct.add(similarity)
                }
            }
            productLikesMatrix.add(likesForProduct)
        }

        // Rekomendasi produk berdasarkan similarity
        val recommendedProducts = recommendProductsBasedOnSimilarity(productLikesMatrix, filteredProducts, topN = 10)

        // Perbarui data rekomendasi menggunakan produk yang direkomendasikan berdasarkan cosine similarity
        rekomendasiAdapter.updateData(recommendedProducts)
    }

    private fun applyFallbackRecommendations(filteredSource: List<Product> = allProducts) {
        // Fallback logic jika produk tidak memiliki cukup likes
        val groupedProducts = filteredSource.groupBy { it.productType }

        val recommendedProducts = groupedProducts.flatMap { (_, productList) ->
            productList.sortedBy { it.pricePerUnit } // Urutkan berdasarkan harga untuk fallback
        }.take(10) // Tampilkan 10 produk teratas berdasarkan harga

        rekomendasiAdapter.updateData(recommendedProducts)
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

    private fun computeCosineSimilarity(productA: Product, productB: Product): Double {
        val commonLikes = getCommonLikes(productA, productB)

        if (commonLikes.isEmpty()) return 0.0

        // Menghitung dot product dan magnitudes (norma)
        val dotProduct = commonLikes.sumBy { userId ->
            (productA.likes[userId] ?: 0) * (productB.likes[userId] ?: 0)
        }

        val magnitudeA = Math.sqrt(commonLikes.sumByDouble {
            Math.pow((productA.likes[it] ?: 0).toDouble(), 2.0)
        })

        val magnitudeB = Math.sqrt(commonLikes.sumByDouble {
            Math.pow((productB.likes[it] ?: 0).toDouble(), 2.0)
        })

        return if (magnitudeA == 0.0 || magnitudeB == 0.0) 0.0 else dotProduct / (magnitudeA * magnitudeB)
    }

    private fun getCommonLikes(productA: Product, productB: Product): List<String> {
        val commonUsers = mutableListOf<String>()
        productA.likes.keys.forEach { userId ->
            if (productB.likes.containsKey(userId)) {
                commonUsers.add(userId)
            }
        }
        return commonUsers
    }

//    private fun getCommonUsers(productA: Product, productB: Product): List<String> {
//
//        val commonUsers = mutableListOf<String>()
//        productA.likes.keys.forEach { userId ->
//            if (productB.likes.containsKey(userId)) {
//                commonUsers.add(userId)
//            }
//        }
//        return commonUsers
//    }

    private fun recommendProductsBasedOnSimilarity(
        matrix: List<List<Double>>,
        filteredProducts: List<Product>,
        topN: Int
    ): List<Product> {
        val recommendedProducts = mutableListOf<Product>()

        // Menghitung rekomendasi berdasarkan similarity
        for (i in matrix.indices) {
            val similarities = matrix[i]

            // Menyortir kemiripan berdasarkan similarity tertinggi
            val sortedSimilarities = similarities.withIndex()
                .sortedByDescending { it.value }
                .take(topN)

            // Menambahkan produk yang mirip
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
                    onLikesCountFetched(0) // Kembalikan 0 jika tidak ada likes
                    return@addOnSuccessListener
                }

                val likesCount = result.size() // Menghitung jumlah likes
                product.likesCount = likesCount // Menyimpan likesCount di objek produk
                // Simpan likes setiap pengguna (userId sebagai key dan jumlah like sebagai value)
                val likesMap = result.associate { doc -> doc.id to 1 }
                product.likes = likesMap
                onLikesCountFetched(likesCount) // Memanggil callback untuk memperbarui likes
            }
            .addOnFailureListener { e ->
                Log.e("KatalogFragment", "Failed to fetch likes count: ${e.message}")
                onLikesCountFetched(0) // Jika gagal, anggap 0 likes
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

        // Update produk list
        produkList.clear()
        produkList.addAll(filtered)
        produkAdapter.notifyDataSetChanged()

        // Update recommendations based on the filtered list
        updateRecommendationsForFiltered(filtered)
    }

    private fun updateRecommendationsForFiltered(filteredProducts: List<Product>) {
        // Filter products with likes
        val recommendedProducts = filteredProducts.filter { it.likesCount > 0 }

        if (recommendedProducts.isNotEmpty()) {
            // Show liked products in the recommendations section
            rekomendasiAdapter.updateData(recommendedProducts)
        } else {
            // Fallback to showing the cheapest products when no likes are available
            applyFallbackRecommendations(filteredProducts)
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}