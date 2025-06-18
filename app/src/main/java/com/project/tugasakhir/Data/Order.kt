package com.project.tugasakhir.Data

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import kotlinx.parcelize.Parcelize

@Parcelize
data class Order(
    var productName: String = "",
    var productType: String = "",
    var quantity: Int = 0,
    var pricePerUnit: Double = 0.0,
    var totalPrice: Double = 0.0,
    var userId: String = "",
    var email: String = "",
    val imageUrl: String = "",
    val description: String = "",
    val imageBase64List: List<String> = emptyList(),
    var userName: String = "",
    var userAddress: String = "",
    var orderNumber: String = "",              // for readable order display only
    var statusOrder: String = "",
    var orderDate: String = "",
    var timestamp: Timestamp? = null,
    var orderTime: String = "",
    @get:Exclude var docId: String = "",      // Firestore document ID, unique key
    var pricePerKg: Double = 0.0
) : Parcelable
