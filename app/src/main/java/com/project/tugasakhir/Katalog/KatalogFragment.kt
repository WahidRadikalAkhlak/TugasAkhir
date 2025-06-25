package com.project.tugasakhir.Katalog

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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

    private lateinit var produkAdapter: ProductImageAdapter
    private lateinit var rekomendasiAdapter: KatalogAdapter

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

        rekomendasiAdapter = KatalogAdapter(produkList, { product ->
            val intent = Intent(context, InfoProductActivity::class.java).apply {
                putExtra("product", product)
                putExtra("source", "catalog")
            }
            context?.startActivity(intent)
        }, true) // Display likes count in recommendations
        binding.rvRekomendasi.adapter = rekomendasiAdapter
        binding.rvRekomendasi.adapter = produkAdapter
        binding.rvRekomendasi.layoutManager = GridLayoutManager(requireContext(), 1, LinearLayoutManager.HORIZONTAL, false) // Horizontal Layout

        setupSearch()
        loadProductsFromFirestore()
    }
    private fun loadProductsFromFirestore() {
        db.collection("products")
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.e("KatalogFragment", "Error listening products", e)
                    return@addSnapshotListener
                }

                snapshots?.let {
                    val productsFromFirestore = it.map { doc -> doc.toObject(Product::class.java) }
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
                            likesCountFetched.add(likesCount)
                            if (likesCountFetched.size == produkList.size) {
                                // All likes have been fetched, now refresh the RecyclerView
                                produkAdapter.notifyDataSetChanged()
                                recommendProductsBasedOnLikes() // Update recommendations after likes are fetched
                            }
                        }
                    }
                }
            }
    }

    private fun getLikesCountForProduct(product: Product, onLikesCountFetched: (Int) -> Unit) {
        db.collection("productLikes")
            .document(product.productName)
            .collection("users")
            .get()
            .addOnSuccessListener { result ->
                val likesCount = result.size() // Count the likes
                product.likesCount = likesCount // Update likesCount in the product object
                onLikesCountFetched(likesCount) // Pass the likes count back to the caller
            }
            .addOnFailureListener { e ->
                Log.e("KatalogFragment", "Failed to fetch likes count: ${e.message}")
                onLikesCountFetched(0) // If failed, assume 0 likes
            }
    }

    private fun updateProductLikeCountInUI(product: Product, likesCount: Int) {
        val index = produkList.indexOf(product)
        if (index != -1) {
            produkList[index].likesCount = likesCount // Update likesCount in the product object
            produkAdapter.notifyItemChanged(index)
        }
    }

    private fun recommendProductsBasedOnLikes() {
        val filteredProducts = allProducts.filter { it.likesCount > 0 }
            .sortedByDescending { it.likesCount }

        rekomendasiAdapter.updateData(filteredProducts)
        rekomendasiAdapter.notifyDataSetChanged()
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

