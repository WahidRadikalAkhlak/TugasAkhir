package com.project.tugasakhir.Account.Penjual

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Adapter.ProductImageAdapter
import com.project.tugasakhir.Cart.KeranjangPenjualActivity
import com.project.tugasakhir.Data.Product
import com.project.tugasakhir.Katalog.Product.InfoProductActivity
import com.project.tugasakhir.Katalog.ProductPenjual.ProductBaruActivity
import com.project.tugasakhir.databinding.ActivityDaftarProductBinding

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
    private var sellerUID: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        // Get user info from Intent
        userEmail = intent.getStringExtra(EXTRA_EMAIL)
        userName = intent.getStringExtra(EXTRA_USERNAME)

        // Get sellerUID from Firebase Authentication
        sellerUID = FirebaseAuth.getInstance().currentUser?.uid

        if (userEmail.isNullOrEmpty()) {
            Toast.makeText(this, "Email user tidak tersedia. Harap login ulang.", Toast.LENGTH_LONG)
                .show()
            finish()
            return
        }

        val displayName = userName ?: "User"
        Toast.makeText(this, "Name: $displayName", Toast.LENGTH_SHORT).show()

        setupRecyclerView()
        setupAddProductButton()
        setupAcceptOrder()
        setupSearchView()
        setupEditBisnis()
    }

    override fun onResume() {
        super.onResume()
        fetchProductData() // This will ensure the list is always fresh
    }

    private fun setupRecyclerView() {
        adapter = ProductImageAdapter(productList) { product ->
            val intent = Intent(this, InfoProductActivity::class.java).apply {
                putExtra("product", product)
                putExtra("source", "seller")
            }
            startActivity(intent)
        }

        binding.rvProductList.apply {
            layoutManager = GridLayoutManager(context, 4, GridLayoutManager.VERTICAL, false)
            adapter = this@DaftarProductActivity.adapter
            setHasFixedSize(true)
        }
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
    private fun setupEditBisnis(){
        binding.btnEditBisnis.setOnClickListener{
            val  intent = Intent(this, DaftarPenjualActivity::class.java).apply {
                putExtra(EXTRA_EMAIL, userEmail)
                putExtra(EXTRA_USERNAME, userName)
            }
            startActivity(intent)
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {
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
        val user = FirebaseAuth.getInstance().currentUser ?: run {
            Toast.makeText(this, "User tidak terdeteksi, harap login ulang.", Toast.LENGTH_SHORT).show()
            return
        }
        val sellerUID = user.uid
        val displayName = user.displayName ?: ""

        // 1) by sellerUID
        db.collection("products")
            .whereEqualTo("sellerUID", sellerUID)
            .get()
            .addOnSuccessListener { docs1 ->
                if (!docs1.isEmpty) {
                    consumeProducts(docs1.toObjects(Product::class.java))
                    return@addOnSuccessListener
                }
                // 2) fallback by email
                db.collection("products")
                    .whereEqualTo("email", email)
                    .get()
                    .addOnSuccessListener { docs2 ->
                        if (!docs2.isEmpty) {
                            consumeProducts(docs2.toObjects(Product::class.java))
                            return@addOnSuccessListener
                        }
                        // 3) fallback by userName (nama tampilan)
                        if (displayName.isNotBlank()) {
                            db.collection("products")
                                .whereEqualTo("userName", displayName)
                                .get()
                                .addOnSuccessListener { docs3 ->
                                    consumeProducts(docs3.toObjects(Product::class.java))
                                }
                                .addOnFailureListener { e -> onFetchError(e) }
                        } else {
                            consumeProducts(emptyList())
                        }
                    }
                    .addOnFailureListener { e -> onFetchError(e) }
            }
            .addOnFailureListener { e -> onFetchError(e) }
    }

    private fun consumeProducts(list: List<Product>) {
        productList.clear()
        productList.addAll(list)
        adapter.notifyDataSetChanged()
        if (productList.isEmpty()) {
            Toast.makeText(this, "Belum ada produk tersedia.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun onFetchError(e: Exception) {
        Log.e("DaftarProductActivity", "Gagal mengambil data produk", e)
        Toast.makeText(this, "Gagal mengambil data produk: ${e.message}", Toast.LENGTH_SHORT).show()
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
