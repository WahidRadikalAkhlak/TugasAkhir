package com.project.tugasakhir.Data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Product(
    val productName: String = "",
    val imageUrls: List<String> = emptyList(),
    val imageBase64List: List<String> = emptyList(),
    val productType: String = "",
    val description: String = "",
    val stockAvailable: Int = 0,
    val pricePerUnit: Double = 0.0,
    val address: String = "",
    var isAvailable: Boolean = true,
    var isRecommended: Boolean = false,
    var isLiked: Boolean = false,
    val distance: Double = Double.MAX_VALUE,
    val userName: String = ""    // tetap dipertahankan
) : Parcelable
