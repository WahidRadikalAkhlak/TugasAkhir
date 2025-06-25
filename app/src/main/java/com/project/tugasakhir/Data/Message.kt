package com.project.tugasakhir.Data

import android.os.Parcel
import android.os.Parcelable

data class Message(
    val senderId: String = "",
    var senderName: String = "",
    val message: String = "",
    val timestamp: Long = 0L
) : Parcelable {

    constructor(parcel: Parcel) : this(
        senderId = parcel.readString() ?: "",
        senderName = parcel.readString() ?: "",
        message= parcel.readString() ?: "",
        timestamp = parcel.readLong()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(senderId)
        parcel.writeString(senderName)
        parcel.writeString(message)
        parcel.writeLong(timestamp)
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
