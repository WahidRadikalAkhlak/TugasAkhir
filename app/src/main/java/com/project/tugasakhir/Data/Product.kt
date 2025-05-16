package com.project.tugasakhir.Data

import java.net.URL

data class Product(
    val productName: String = "",
    val imageUrl: String = "",
    val productType: String = "",
    val description: String = "",
    val stockAvailable: Int = 0,
    val pricePerUnit: Double = 0.0,
    val isAvailable: Boolean = true,
    val isRecommended: Boolean = false,
    val isLiked: Boolean = false,
    val distance: Double = Double.MAX_VALUE
)


