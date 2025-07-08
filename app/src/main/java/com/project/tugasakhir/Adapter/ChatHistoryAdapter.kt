package com.project.tugasakhir.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.project.tugasakhir.Data.Message
import com.project.tugasakhir.R
import com.project.tugasakhir.databinding.ItemChatBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatHistoryAdapter(
    private val chats: List<Message>,  // List of chats to display
    private val currentUserId: String,
    private val onItemClickListener: (Message) -> Unit  // Listener for chat click
) : RecyclerView.Adapter<ChatHistoryAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(private val binding: ItemChatBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(chat: Message) {
            // Set the receiver's name for the chat listing
            binding.pengguna.text = chat.receiverName  // Display receiverName in the view

            // Show the message content
            binding.isiPesan.text = chat.message

            // Set profile image (replace with actual user profile image if available)
            binding.iconProfil.setImageResource(R.drawable.account_circle)

            // Format the timestamp
            val formattedTime =
                SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(chat.timestamp))
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