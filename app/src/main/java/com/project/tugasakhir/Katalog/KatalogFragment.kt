package com.project.tugasakhir.Katalog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.FragmentKatalogBinding
import com.project.tugasakhir.Data.Product
import com.project.tugasakhir.databinding.ItemProductBinding

class KatalogFragment : Fragment(R.layout.fragment_katalog) {

    private var _binding: FragmentKatalogBinding? = null
    private val binding get() = _binding!!

    private lateinit var terdekatAdapter: KatalogAdapter
    private lateinit var rekomendasiAdapter: KatalogAdapter
    private lateinit var sukaiAdapter: KatalogAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentKatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set LayoutManager for RecyclerView
        binding.terdekatRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rekomendasiRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.sukaiRecyclerView.layoutManager =
            LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)

        // Initialize Adapter with mock data
        terdekatAdapter = KatalogAdapter(getMockData())
        rekomendasiAdapter = KatalogAdapter(getMockData())
        sukaiAdapter = KatalogAdapter(getMockData())

        // Set the adapters to RecyclerView
        binding.terdekatRecyclerView.adapter = terdekatAdapter
        binding.rekomendasiRecyclerView.adapter = rekomendasiAdapter
        binding.sukaiRecyclerView.adapter = sukaiAdapter
    }

    // Provide mock data for the RecyclerView
    private fun getMockData(): List<Product> {
        return listOf(
            Product("Product 1", "Rp 100.000", "product_image_1"),
            Product("Product 2", "Rp 150.000", "product_image_2"),
            Product("Product 3", "Rp 200.000", "product_image_3")
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Adapter for RecyclerView
class KatalogAdapter(private val productList: List<Product>) :
    RecyclerView.Adapter<KatalogAdapter.KatalogViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KatalogViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return KatalogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: KatalogViewHolder, position: Int) {
        val product = productList[position]
        holder.bind(product)
    }

    override fun getItemCount(): Int = productList.size

    inner class KatalogViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // Bind data to views
        fun bind(product: Product) {
            binding.titleProduct.text = product.name
            binding.harga.text = product.price
            // Example for loading images using Glide (You can use Picasso as well)
            Glide.with(binding.imgProduct.context)
                .load(product.imageResId) // Assuming imageResId is a URL or drawable name
                .into(binding.imgProduct)
        }
    }
}
