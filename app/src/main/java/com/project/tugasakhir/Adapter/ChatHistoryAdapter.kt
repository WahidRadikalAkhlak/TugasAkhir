package com.project.tugasakhir.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.project.tugasakhir.Data.Message
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ItemChatBinding
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

class ChatHistoryAdapter(
    private val chats: List<Message>,  // List of chats to display
    private val currentUserId: String,
    private val onItemClickListener: (Message) -> Unit  // Listener for chat click
) : RecyclerView.Adapter<ChatHistoryAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(private val binding: ItemChatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chat: Message) {
            // Set the sender's or receiver's name
            if (chat.senderId == currentUserId) {
                binding.pengguna.text = chat.receiverName  // If the message is from the current user, show receiver's name
            } else {
                binding.pengguna.text = chat.senderName  // If the message is from the other user, show sender's name
            }

            // Show the message content
            binding.isiPesan.text = chat.message

            // Set profile image (you can replace it with actual user profile images)
            binding.iconProfil.setImageResource(R.drawable.account_circle)

            // Format the timestamp
            val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(chat.timestamp))
            binding.timestamp.text = formattedTime

            // Handle item click to open chat detail
            itemView.setOnClickListener {
                onItemClickListener(chat)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val chat = chats[position]
        holder.bind(chat)
    }

    override fun getItemCount(): Int = chats.size
}
