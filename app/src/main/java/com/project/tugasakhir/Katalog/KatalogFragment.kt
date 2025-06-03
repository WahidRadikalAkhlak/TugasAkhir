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

    private lateinit var produkAdapter: ProductImageAdapter
    private lateinit var rekomendasiAdapter: KatalogAdapter
    private val produkList = mutableListOf<Product>()

    private val db = FirebaseFirestore.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentKatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        produkAdapter = ProductImageAdapter(produkList) { product ->
            val intent = Intent(context, InfoProductActivity::class.java).apply {
                putExtra("product", product)      // Kirim objek produk
                putExtra("source", "catalog")     // Tandai asalnya dari katalog (beli produk)
            }
            context?.startActivity(intent)
        }
        binding.rvProdukList.adapter = produkAdapter
        binding.rvProdukList.layoutManager = GridLayoutManager(requireContext(), 2)

        rekomendasiAdapter = KatalogAdapter()
        binding.rekomendasiRecyclerView.layoutManager = GridLayoutManager(requireContext(), 2, LinearLayoutManager.HORIZONTAL, false)
        binding.rekomendasiRecyclerView.adapter = rekomendasiAdapter

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

                    produkList.clear()              // **Tambah ini**
                    produkList.addAll(productsFromFirestore)  // **Tambah ini**

                    produkAdapter.notifyDataSetChanged()


                    val userLikedProducts = allProducts.filter { it.isLiked }
                    val rekomendasiList = getItemBasedRecommendations(allProducts, userLikedProducts)
                    rekomendasiAdapter.updateData(rekomendasiList)
                }
            }
    }

    private fun getItemBasedRecommendations(
        allProducts: List<Product>,
        userLikedProducts: List<Product>
    ): List<Product> {
        if (userLikedProducts.isEmpty()) return emptyList()

        val likedTypes = userLikedProducts.map { it.productType }.toSet()

        return allProducts.filter {
            it.productType in likedTypes &&
                    userLikedProducts.none { liked -> liked.productName == it.productName }
        }.take(10)
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
        produkList.clear()               // Tambah ini
        produkList.addAll(filteredProducts)   // Tambah ini
        produkAdapter.notifyDataSetChanged()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
