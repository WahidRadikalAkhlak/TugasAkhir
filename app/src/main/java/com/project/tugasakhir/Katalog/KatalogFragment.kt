package com.project.tugasakhir.Katalog

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.analytics.ecommerce.Product
import com.project.tugasakhir.Adapter.ProductAdapter
import com.project.tugasakhir.R

class KatalogFragment : Fragment(R.layout.fragment_katalog) {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ProductAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        recyclerView = view.findViewById(R.id.rv_katalog)
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        adapter = ProductAdapter()
        recyclerView.adapter = adapter

        // Load your data and set it to the adapter
        val productList = getProductList()
        adapter.submitList(productList)
    }

    private fun getProductList(): List<Product> {
        // Return your list of products
        return listOf(
            Product("Product 1", "Description 1", 100),
            Product("Product 2", "Description 2", 200),
            Product("Product 3", "Description 3", 300)
        )
    }
}
