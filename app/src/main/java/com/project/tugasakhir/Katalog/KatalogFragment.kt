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
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Data.Product
import com.project.tugasakhir.databinding.FragmentKatalogBinding
import com.project.tugasakhir.Adapter.KatalogAdapter
import com.project.tugasakhir.Adapter.ProductImageAdapter
import com.project.tugasakhir.Katalog.Product.InfoProductActivity

class KatalogFragment : Fragment() {
    private var _binding: FragmentKatalogBinding? = null
    private val binding get() = _binding!!

    private val allProducts = mutableListOf<Product>()
    private val filteredProducts = mutableListOf<Product>()
    private val produkList = mutableListOf<Product>()

    private lateinit var produkAdapter: ProductImageAdapter // Adapter for product list
    private lateinit var rekomendasiAdapter: KatalogAdapter // Adapter for recommendations

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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
        binding.rvProdukList.adapter = produkAdapter
        binding.rvProdukList.layoutManager = GridLayoutManager(requireContext(), 2)

        // Adapter for recommendations (with likes count)
        rekomendasiAdapter = KatalogAdapter(mutableListOf(), { product ->
            val intent = Intent(context, InfoProductActivity::class.java).apply {
                putExtra("product", product)
                putExtra("source", "catalog")
            }
            context?.startActivity(intent)
        }, true) // Display likes count in recommendations

        binding.rvRekomendasi.adapter = rekomendasiAdapter // Set rekomendasiAdapter to rvRekomendasi
        binding.rvRekomendasi.layoutManager = GridLayoutManager(requireContext(), 1, GridLayoutManager.HORIZONTAL, false) // Horizontal Layout

        setupSearch()
        loadProductsFromFirestore() // Load products from Firestore
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

                // Fetch likes count for each product and update UI
                val likesCountFetched = mutableListOf<Int>()
                for (product in produkList) {
                    getLikesCountForProduct(product) { likesCount ->
                        product.likesCount = likesCount
                        likesCountFetched.add(likesCount)
                        if (likesCountFetched.size == produkList.size) {
                            // All likes have been fetched, now refresh the RecyclerView
                            produkAdapter.notifyDataSetChanged()
                            applyCollaborativeFiltering()  // Apply the collaborative filtering after likes are fetched
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal mengambil data produk: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun applyCollaborativeFiltering() {
        // Check if there are enough products to apply collaborative filtering
        if (allProducts.isEmpty()) {
            Log.e("KatalogFragment", "No products available for collaborative filtering")
            return // If no products, do not proceed with collaborative filtering
        }

        // If there are no likes or products with likes, use fallback recommendations
        if (allProducts.all { it.likesCount == 0 }) {
            Log.e("KatalogFragment", "No likes available, applying fallback recommendations")
            applyFallbackRecommendations()
        } else {
            // Apply cosine similarity when likes are available
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

            // Ensure there are products with more than 3 likes
            if (filteredProducts.isEmpty()) {
                Log.e("KatalogFragment", "No products with more than 3 likes")
                applyFallbackRecommendations()
                return // If no products with more than 3 likes, use fallback recommendations
            }

            // Get recommendations based on cosine similarity
            val recommendedProducts = recommendProductsBasedOnSimilarity(productLikesMatrix, filteredProducts)

            // Update adapter to display recommendations
            rekomendasiAdapter.updateData(recommendedProducts)
        }
    }

    // Fallback function for when there is no enough data for collaborative filtering
    private fun applyFallbackRecommendations() {
        // Use the category or random selection for fallback recommendations
        val recommendedProducts = allProducts.take(3) // Take the first 3 products (or randomly if there are more)

        // Update the adapter with fallback recommendations
        rekomendasiAdapter.updateData(recommendedProducts)
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

    private fun recommendProductsBasedOnSimilarity(matrix: List<List<Double>>, filteredProducts: List<Product>): List<Product> {
        val recommendedProducts = mutableListOf<Product>()

        for (i in matrix.indices) {
            val similarities = matrix[i]

            val sortedSimilarities = similarities.withIndex()
                .sortedByDescending { it.value }
                .take(3)

            for (similarity in sortedSimilarities) {
                if (similarity.value > 0) {
                    val recommendedProduct = filteredProducts[similarity.index]
                    if (!recommendedProducts.contains(recommendedProduct)) {
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
            filterProductList(query)
        }
    }

    private fun filterProductList(query: String) {
        filteredProducts.clear()
        if (query.isEmpty()) {
            filteredProducts.addAll(allProducts)
        } else {
            filteredProducts.addAll(
                allProducts.filter {
                    it.productName.contains(query, ignoreCase = true) ||
                            it.productType.contains(query, ignoreCase = true)
                }
            )
        }
        produkList.clear()
        produkList.addAll(filteredProducts)
        produkAdapter.notifyDataSetChanged()

        filterRecommendationList(query)
    }

    private fun filterRecommendationList(query: String) {
        val recommendedProducts = allProducts.filter { it.likesCount > 2 }
        filteredProducts.clear()
        if (query.isEmpty()) {
            filteredProducts.addAll(recommendedProducts)
        } else {
            filteredProducts.addAll(
                recommendedProducts.filter {
                    it.productName.contains(query, ignoreCase = true) ||
                            it.productType.contains(query, ignoreCase = true)
                }
            )
        }
        rekomendasiAdapter.updateData(filteredProducts) // Update rvRekomendasi
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}