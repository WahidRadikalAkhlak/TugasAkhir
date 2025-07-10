package com.project.tugasakhir.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.project.tugasakhir.Data.Review
import com.project.tugasakhir.R

class UlasanAdapter(private val reviews: List<Review>) : RecyclerView.Adapter<UlasanAdapter.UlasanViewHolder>() {

    inner class UlasanViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val userNameTextView: TextView = itemView.findViewById(R.id.tv_user_name)
        val ratingKomunikasiBar: RatingBar = itemView.findViewById(R.id.ratingbarkomunikasi)
        val ratingKualitasBar: RatingBar = itemView.findViewById(R.id.ratingbarkualitas)
        val commentTextView: TextView = itemView.findViewById(R.id.tv_comment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UlasanViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_ulasan, parent, false)
        return UlasanViewHolder(view)
    }

    override fun onBindViewHolder(holder: UlasanViewHolder, position: Int) {
        val review = reviews[position]
        holder.userNameTextView.text = review.userName
        holder.ratingKomunikasiBar.rating = review.ratingKomunikasi
        holder.ratingKualitasBar.rating = review.ratingKualitas
        holder.commentTextView.text = review.komentar
    }

    override fun getItemCount(): Int {
        return reviews.size
    }
}
