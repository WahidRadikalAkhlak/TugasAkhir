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
                            applyCollaborativeFiltering()
                            updateRecommendations()
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    requireContext(),
                    "Gagal mengambil data produk: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun updateRecommendations() {
        // Group products by product type
        val groupedProducts = allProducts.groupBy { it.productType }

        val recommendedProducts = mutableListOf<Product>()

        // Iterate through each product type
        for ((type, productsInCategory) in groupedProducts) {
            val productWithLikes = productsInCategory.filter { it.likesCount >= 3 }

            if (productWithLikes.isNotEmpty()) {
                // Add products with at least 3 likes to recommendations
                recommendedProducts.addAll(productWithLikes)
            } else {
                // If no product has likes >= 3, show the cheapest product
                val cheapestProduct = productsInCategory.minByOrNull { it.pricePerUnit }
                if (cheapestProduct != null) {
                    recommendedProducts.add(cheapestProduct)
                }
            }
        }

        rekomendasiAdapter.updateData(recommendedProducts)
    }

    private fun applyCollaborativeFiltering() {
        if (allProducts.isEmpty()) {
            Log.e("KatalogFragment", "No products available for collaborative filtering")
            return
        }

        if (allProducts.all { it.likesCount == 0 }) {
            Log.e("KatalogFragment", "No likes available, applying fallback recommendations")
            applyFallbackRecommendations()
            return
        }

        val productLikesMatrix = mutableListOf<MutableList<Double>>()
        for (i in allProducts.indices) {
            val likesForProduct = mutableListOf<Double>()
            for (j in allProducts.indices) {
                if (i != j) {
                    val similarity = computeCosineSimilarity(allProducts[i], allProducts[j])
                    likesForProduct.add(similarity)
                }
            }
            productLikesMatrix.add(likesForProduct)
        }

        val filteredProducts = allProducts.filter { it.likesCount > 3 }

        if (filteredProducts.isEmpty()) {
            Log.e("KatalogFragment", "No products with more than 3 likes")
            applyFallbackRecommendations()
            return
        }

        // Increase the number of recommended products from 3 to a larger number (e.g., 10)
        val recommendedProducts =
            recommendProductsBasedOnSimilarity(productLikesMatrix, filteredProducts, topN = 10)
        rekomendasiAdapter.updateData(recommendedProducts)
    }

    private fun applyFallbackRecommendations(filteredSource: List<Product> = allProducts) {
        // Fallback logic if products don't have enough likes
        val groupedProducts = filteredSource.groupBy { it.productType }

        val recommendedProducts = groupedProducts.flatMap { (_, productList) ->
            productList.sortedBy { it.pricePerUnit } // Sort by price for the fallback
        }.take(10) // Show top 10 based on price

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

        val commonUsers = getCommonUsers(productA, productB)

        if (commonUsers.isEmpty()) {
            return 0.0
        }

        // Menghitung dot product dan magnitudes (norma)
        val dotProduct = commonUsers.sumBy { userId ->
            (productA.likes[userId] ?: 0) * (productB.likes[userId] ?: 0)
        }

        val magnitudeA = Math.sqrt(commonUsers.sumByDouble {
            Math.pow((productA.likes[it] ?: 0).toDouble(), 2.0)
        })

        val magnitudeB = Math.sqrt(commonUsers.sumByDouble {
            Math.pow((productB.likes[it] ?: 0).toDouble(), 2.0)
        })

        return if (magnitudeA == 0.0 || magnitudeB == 0.0) 0.0 else dotProduct / (magnitudeA * magnitudeB)
    }


    private fun getCommonUsers(productA: Product, productB: Product): List<String> {

        val commonUsers = mutableListOf<String>()
        productA.likes.keys.forEach { userId ->
            if (productB.likes.containsKey(userId)) {
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

            // Cek apakah filteredProducts cukup besar untuk diakses
            if (filteredProducts.size <= similarities.size) {
                val sortedSimilarities = similarities.withIndex()
                    .sortedByDescending { it.value }
                    .take(topN) // Ambil 'topN' produk teratas yang mirip

                for (similarity in sortedSimilarities) {
                    if (similarity.value > 0) {
                        val recommendedProduct = filteredProducts.getOrNull(similarity.index)
                        if (recommendedProduct != null && !recommendedProducts.contains(
                                recommendedProduct
                            )
                        ) {
                            recommendedProducts.add(recommendedProduct)
                        }
                    }
                }
            } else {
                Log.e(
                    "KatalogFragment",
                    "Mismatch in size between similarity matrix and filtered products list."
                )
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
            (selectedChipType.isEmpty() || it.productType.equals(
                selectedChipType,
                ignoreCase = true
            )) &&
                    (searchQuery.isEmpty() || it.productName.contains(
                        searchQuery,
                        ignoreCase = true
                    ))
        }

        // Update produk list
        produkList.clear()
        produkList.addAll(filtered)
        produkAdapter.notifyDataSetChanged()

        // Update rekomendasi
        val recommended = filtered.filter { it.likesCount >= 3 }

        if (recommended.isNotEmpty()) {
            rekomendasiAdapter.updateData(recommended)
        } else {
            applyFallbackRecommendations(filtered)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}