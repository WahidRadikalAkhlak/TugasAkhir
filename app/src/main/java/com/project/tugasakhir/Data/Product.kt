package com.project.tugasakhir.Data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class Product(
    val productId: String = UUID.randomUUID().toString(), // Unique product ID
    val productName: String = "",
    val imageUrls: List<String> = emptyList(),
    val imageBase64List: List<String> = emptyList(),
    val productType: String = "",
    val description: String = "",
    val stockAvailable: Int = 0,
    val pricePerUnit: Double = 0.0,
    val alamatToko: String = "",
    var isAvailable: Boolean = false,
    var isRecommended: Boolean = false,
    var isLiked: Boolean = false,
    var likesCount: Int = 0, // Likes count for the product
    val distance: Double = Double.MAX_VALUE,
    val discount: Double = 0.0,
    val userName: String = "",
    var likes: Map<String, Int> = emptyMap(),
    val email: String = "",
    var sellerUID: String = ""
) : Parcelable {

    fun getIsAvailable(): Boolean {
        return isAvailable
    }

    fun setIsAvailable(value: Boolean) {
        isAvailable = value
    }
}
