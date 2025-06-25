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

class ChatAdapter(
    private val messages: MutableList<Message>,
    private val onItemClickListener: (Message) -> Unit
) : RecyclerView.Adapter<ChatAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(private val binding: ItemChatBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.pengguna.text = message.senderName
            binding.isiPesan.text = message.message
            binding.iconProfil.setImageResource(R.drawable.account_circle)  // Placeholder icon

            // Format the timestamp and display it
            val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
            binding.timestamp.text = formattedTime

            // Set item click listener
            itemView.setOnClickListener {
                onItemClickListener(message)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemChatBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    override fun getItemCount(): Int = messages.size
}
