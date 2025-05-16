package com.project.tugasakhir.Katalog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Data.Product
import com.project.tugasakhir.databinding.FragmentKatalogBinding
import com.project.tugasakhir.databinding.ItemProductBinding
import androidx.recyclerview.widget.RecyclerView

class KatalogFragment : Fragment() {

    private var _binding: FragmentKatalogBinding? = null
    private val binding get() = _binding!!

    private lateinit var terdekatAdapter: KatalogAdapter
    private lateinit var rekomendasiAdapter: KatalogAdapter
    private lateinit var sukaiAdapter: KatalogAdapter

    private val db = FirebaseFirestore.getInstance()

    private val terdekatList = mutableListOf<Product>()
    private val rekomendasiList = mutableListOf<Product>()
    private val sukaiList = mutableListOf<Product>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentKatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup LayoutManager horizontal untuk ketiga RecyclerView
        binding.terdekatRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.rekomendasiRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.sukaiRecyclerView.layoutManager =
            LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)

        // Setup adapter dengan list mutable
        terdekatAdapter = KatalogAdapter(terdekatList)
        rekomendasiAdapter = KatalogAdapter(rekomendasiList)
        sukaiAdapter = KatalogAdapter(sukaiList)

        binding.terdekatRecyclerView.adapter = terdekatAdapter
        binding.rekomendasiRecyclerView.adapter = rekomendasiAdapter
        binding.sukaiRecyclerView.adapter = sukaiAdapter

        loadProductsFromFirestore()
    }

    private fun loadProductsFromFirestore() {
        db.collection("products")
            .get()
            .addOnSuccessListener { documents ->
                val allProducts = documents.map { it.toObject(Product::class.java) }

                terdekatList.clear()
                rekomendasiList.clear()
                sukaiList.clear()

                terdekatList.addAll(allProducts.filter { it.distance <= 5.0 })  // radius 5 km
                rekomendasiList.addAll(allProducts.filter { it.isRecommended })
                sukaiList.addAll(allProducts.filter { it.isLiked })

                terdekatAdapter.notifyDataSetChanged()
                rekomendasiAdapter.notifyDataSetChanged()
                sukaiAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener {
                // Tangani error, misal tampilkan Toast
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// Adapter untuk menampilkan list produk
class KatalogAdapter(private val productList: MutableList<Product>) :
    RecyclerView.Adapter<KatalogAdapter.KatalogViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KatalogViewHolder {
        val binding = ItemProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return KatalogViewHolder(binding)
    }

    override fun onBindViewHolder(holder: KatalogViewHolder, position: Int) {
        holder.bind(productList[position])
    }

    override fun getItemCount(): Int = productList.size

    inner class KatalogViewHolder(private val binding: ItemProductBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.tvProductName.text = product.productName
            binding.HargaBarang.text = product.pricePerUnit.toString()

            Glide.with(binding.imgProduct.context)
                .load(product.imageUrl)
                .into(binding.imgProduct)
        }
    }
}
