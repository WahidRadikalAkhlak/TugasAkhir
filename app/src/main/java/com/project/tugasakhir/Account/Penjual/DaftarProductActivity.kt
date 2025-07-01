package com.project.tugasakhir.Account.Penjual

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Adapter.ProductImageAdapter
import com.project.tugasakhir.Cart.KeranjangPenjualActivity
import com.project.tugasakhir.Data.Product
import com.project.tugasakhir.Katalog.Product.InfoProductActivity
import com.project.tugasakhir.databinding.ActivityDaftarProductBinding
import com.project.tugasakhir.Katalog.ProductPenjual.ProductBaruActivity

class DaftarProductActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_EMAIL = "EMAIL"
        const val EXTRA_USERNAME = "USERNAME"
        const val EXTRA_PRODUCT = "product"
    }

    private val binding: ActivityDaftarProductBinding by lazy {
        ActivityDaftarProductBinding.inflate(layoutInflater)
    }
    private val db: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private val productList = mutableListOf<Product>()
    private lateinit var adapter: ProductImageAdapter

    private var userEmail: String? = null
    private var userName: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        userEmail = intent.getStringExtra(EXTRA_EMAIL)
        userName = intent.getStringExtra(EXTRA_USERNAME)

        if (userEmail.isNullOrEmpty()) {
            Toast.makeText(this, "Email user tidak tersedia. Harap login ulang.", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        val displayName = userName ?: "User"
        Toast.makeText(this, "Name: $displayName", Toast.LENGTH_SHORT).show()


        setupRecyclerView()
        setupAddProductButton()
        setupAcceptOrder()
        setupSearchView()
    }

    override fun onResume() {
        super.onResume()
        fetchProductData()
    }

    private fun setupRecyclerView() {
        // Mengubah RecyclerView untuk menampilkan item secara horizontal
        adapter = ProductImageAdapter(productList) { product ->
            val intent = Intent(this, InfoProductActivity::class.java).apply {
                putExtra("product", product)      // Kirim objek produk
                putExtra("source", "seller")      // Tandai asalnya dari daftar produk penjual (edit/hapus)
            }
            startActivity(intent)
        }

        // Ubah GridLayoutManager menjadi LinearLayoutManager dengan orientasi horizontal
        binding.rvProductList.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        binding.rvProductList.adapter = adapter
    }

    private fun setupAddProductButton() {
        binding.btnAddProduct.setOnClickListener {
            val intent = Intent(this, ProductBaruActivity::class.java).apply {
                putExtra(EXTRA_EMAIL, userEmail)
                putExtra(EXTRA_USERNAME, userName)
            }
            startActivity(intent)
        }
    }

    private fun setupAcceptOrder() {
        binding.btnAcceptOrder.setOnClickListener {
            val intent = Intent(this, KeranjangPenjualActivity::class.java).apply {
                putExtra(EXTRA_EMAIL, userEmail)
                putExtra(EXTRA_USERNAME, userName)
            }
            startActivity(intent)
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                filterProductList(newText)
                return true
            }
        })
    }

    private fun fetchProductData() {
        val email = userEmail ?: run {
            Toast.makeText(this, "Email user tidak valid", Toast.LENGTH_SHORT).show()
            return
        }
        db.collection("products")
            .whereEqualTo("email", email)  // Ganti userId dengan email
            .get()
            .addOnSuccessListener { documents ->
                val productsFromFirestore = documents.mapNotNull { doc ->
                    doc.toObject(Product::class.java)
                }
                productList.clear()
                productList.addAll(productsFromFirestore)
                adapter.notifyDataSetChanged()

                if (productList.isEmpty()) {
                    Toast.makeText(this, "Belum ada produk tersedia.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("DaftarProductActivity", "Gagal mengambil data produk", e)
                Toast.makeText(this, "Gagal mengambil data produk: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterProductList(query: String?) {
        if (query.isNullOrBlank()) {
            adapter.updateData(productList)  // If the query is empty, show all products
        } else {
            val filteredList = productList.filter { product ->
                product.productName.contains(query, ignoreCase = true) ||  // Filter by product name
                        product.productType.contains(query, ignoreCase = true)    // Filter by product type
            }
            adapter.updateData(filteredList)  // Update the adapter with the filtered list
        }
    }
}
