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
    private val chats: List<Message>,  // Daftar chat yang akan ditampilkan
    private val currentUserId: String,
    private val onItemClickListener: (Message) -> Unit  // Listener untuk chat yang diklik
) : RecyclerView.Adapter<ChatHistoryAdapter.MessageViewHolder>() {

    inner class MessageViewHolder(private val binding: ItemChatBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(chat: Message) {
            // Menentukan siapa yang mengirim pesan berdasarkan senderId dan receiverId
            if (chat.senderId == currentUserId) {
                binding.pengguna.text = chat.receiverName // Jika pesan berasal dari user, tampilkan receiverName
            } else {
                binding.pengguna.text = chat.senderName // Jika pesan dari penerima, tampilkan senderName
            }

            // Tampilkan isi pesan atau ringkasan pesan
            binding.isiPesan.text = chat.message

            // Set gambar profil (placeholder)
            binding.iconProfil.setImageResource(R.drawable.account_circle)

            // Format waktu pesan
            val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(chat.timestamp))
            binding.timestamp.text = formattedTime

            // Klik item untuk membuka chat detail
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
