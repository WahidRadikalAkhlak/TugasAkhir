package com.project.tugasakhir.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.project.tugasakhir.Data.Message
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ItemTextMessageBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatSendAdapter(
    private val messages: List<Message>,  // Daftar pesan yang dikirim
    private val currentUserId: String
) : RecyclerView.Adapter<ChatSendAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(private val binding: ItemTextMessageBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            // Menyesuaikan apakah pesan dikirim oleh pengguna saat ini
            if (message.senderId == currentUserId) {
                // Apply gradient for sender's message
                binding.messageroot.setBackgroundResource(R.drawable.gradient_profile)
            } else {
                // Apply gradient for receiver's message
                binding.messageroot.setBackgroundResource(R.drawable.gradient_profile)
            }

            // Mengatur teks pesan
            binding.tvMessageText.text = message.message

            // Memformat waktu pesan
            val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
            binding.tvMessageTime.text = formattedTime
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemTextMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    override fun getItemCount(): Int = messages.size
}
