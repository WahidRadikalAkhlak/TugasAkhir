package com.project.tugasakhir.Account.Penjual

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Adapter.ProductImageAdapter
import com.project.tugasakhir.Data.Product
import com.project.tugasakhir.databinding.ActivityDaftarProductBinding
import com.project.tugasakhir.Katalog.ProductPenjual.ProductBaruActivity

class DaftarProductActivity : AppCompatActivity() {
    private lateinit var binding: ActivityDaftarProductBinding
    private val db = FirebaseFirestore.getInstance()

    private val productList = mutableListOf<Product>()
    private lateinit var adapter: ProductImageAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDaftarProductBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Inisialisasi adapter dan RecyclerView dengan GridLayout 2 kolom
        adapter = ProductImageAdapter(productList)
        binding.rvProductList.layoutManager = GridLayoutManager(this, 2)
        binding.rvProductList.adapter = adapter

        // Tombol tambah produk
        binding.btnAddProduct.setOnClickListener {
            val intent = Intent(this, ProductBaruActivity::class.java)
            startActivity(intent)
        }

        // Load data awal dari Firestore
        fetchProductData()

        // Setup searchView untuk filter produk
        setupSearch()
    }

    override fun onResume() {
        super.onResume()
        fetchProductData()  // Refresh data setiap kali activity muncul kembali
    }

    private fun fetchProductData() {
        db.collection("products")
            .get()
            .addOnSuccessListener { documents ->
                val firestoreProductList = documents.map { it.toObject(Product::class.java) }

                productList.clear()
                productList.addAll(firestoreProductList)

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load products: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupSearch() {
        binding.searchView.setOnQueryTextListener(object : androidx.appcompat.widget.SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                filterProductList(newText)
                return true
            }
        })
    }

    private fun filterProductList(query: String?) {
        val filteredList = if (query.isNullOrEmpty()) {
            productList
        } else {
            productList.filter {
                it.productName.contains(query, ignoreCase = true) ||
                        it.productType.contains(query, ignoreCase = true)
            }
        }
        adapter.updateData(filteredList)
    }
}
