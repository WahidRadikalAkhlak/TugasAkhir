package com.project.tugasakhir.Data

import android.os.Parcel
import android.os.Parcelable

data class Message(
    val senderId: String = "",
    var senderName: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    val participants: List<String> = listOf(),
    val receiverId: String = "",  // Add receiverId for the product owner's username
    var receiverName: String = "", // Add receiverName for the product owner's name
    val chatId: String = ""
) : Parcelable {

    constructor(parcel: Parcel) : this(
        senderId = parcel.readString() ?: "",
        senderName = parcel.readString() ?: "",
        message = parcel.readString() ?: "",
        timestamp = parcel.readLong(),
        receiverId = parcel.readString() ?: "",
        receiverName = parcel.readString() ?: "",
        chatId = parcel.readString() ?: ""  // Read chatId from Parcel
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(senderId)
        parcel.writeString(senderName)
        parcel.writeString(message)
        parcel.writeLong(timestamp)
        parcel.writeString(receiverId)
        parcel.writeString(receiverName)
        parcel.writeString(chatId) // Write chatId to Parcel
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Message> {
        override fun createFromParcel(parcel: Parcel): Message {
            return Message(parcel)
        }

        override fun newArray(size: Int): Array<Message?> {
            return arrayOfNulls(size)
        }
    }
}
