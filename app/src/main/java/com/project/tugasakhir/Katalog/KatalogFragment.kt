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
                val productsFromFirestore = snapshots.map { doc -> doc.toObject(Product::class.java) }
                    .filter { product -> product.isAvailable }

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
                        product.likesCount = likesCount // Update likesCount on the product object

                        likesCountFetched.add(likesCount)
                        if (likesCountFetched.size == produkList.size) {
                            // All likes have been fetched, now refresh the RecyclerView
                            produkAdapter.notifyDataSetChanged()
                            recommendProductsBasedOnLikes() // Update recommendations after likes are fetched
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Gagal mengambil data produk: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getLikesCountForProduct(product: Product, onLikesCountFetched: (Int) -> Unit) {
        db.collection("productLikes")
            .document(product.productName) // Access the product by its name
            .collection("users")
            .get()
            .addOnSuccessListener { result ->
                val likesCount = result.size() // Count the likes (documents in the "users" collection)
                product.likesCount = likesCount // Set likesCount on the product object
                onLikesCountFetched(likesCount) // Pass the likes count back to the caller
            }
            .addOnFailureListener { e ->
                Log.e("KatalogFragment", "Failed to fetch likes count: ${e.message}")
                onLikesCountFetched(0) // If failed, assume 0 likes
            }
    }

    private fun recommendProductsBasedOnLikes() {
        // Filter products where likesCount > 2 and sort by likesCount in descending order
        val recommendedProducts = allProducts.filter { it.likesCount > 2 }
            .sortedByDescending { it.likesCount }

        rekomendasiAdapter.updateData(recommendedProducts) // Update the recommendation list
        rekomendasiAdapter.notifyDataSetChanged() // Notify adapter to refresh the view
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
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
