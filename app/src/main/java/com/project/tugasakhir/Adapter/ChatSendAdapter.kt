package com.project.tugasakhir.Adapter

import android.view.LayoutInflater
import android.view.View
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

    inner class MessageViewHolder(private val binding: ItemTextMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(message: Message) {
            // Menyesuaikan apakah pesan dikirim oleh pengguna saat ini
            if (message.senderId == currentUserId) {
                // Set the sender's message layout to the right
                binding.messageroot.visibility = View.VISIBLE
                binding.receiverMessageRoot.visibility = View.GONE
                binding.messageroot.setBackgroundResource(R.drawable.gradient_profile)  // Set specific background for sender's message
                binding.tvMessageText.text = message.message
                val formattedTime =
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
                binding.tvMessageTime.text = formattedTime
            } else {
                // Set the receiver's message layout to the left
                binding.receiverMessageRoot.visibility = View.VISIBLE
                binding.messageroot.visibility = View.GONE
                binding.receiverMessageRoot.setBackgroundResource(R.drawable.white_field_background)  // Set specific background for receiver's message
                binding.tvReceiverMessageText.text = message.message
                val formattedTime =
                    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))
                binding.tvReceiverMessageTime.text = formattedTime
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding =
            ItemTextMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    override fun getItemCount(): Int = messages.size
}
