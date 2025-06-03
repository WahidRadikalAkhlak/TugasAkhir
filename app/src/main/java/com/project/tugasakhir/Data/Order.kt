package com.project.tugasakhir.Data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Order(
    val productName: String = "",
    val productType: String = "",
    val quantity: Int = 0,          // Jumlah produk yang dibeli
    val pricePerUnit: Double = 0.0, // Harga per unit
    val totalPrice: Double = 0.0,  // quantity * pricePerUnit
    val imageUrl: String = "",     // URL gambar produk (ambil dari katalog)
    val userName: String = "",     // Nama penjual
    val userAddress: String = "",  // Alamat penjual
    val orderNumber: String = "",  // Nomor order
    val status: String = "",       // Status order
    val orderDate: String = "",    // Tanggal order
    val orderTime: String = ""     // Waktu order
) : Parcelable



