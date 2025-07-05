package com.project.tugasakhir.Chat.pesan

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Adapter.ChatSendAdapter
import com.project.tugasakhir.Data.Message
import com.project.tugasakhir.Data.Product
import com.project.tugasakhir.databinding.ActivityPesanBinding

class PesanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPesanBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private var chatId: String? = null
    private lateinit var messageAdapter: ChatSendAdapter
    private val messageList = mutableListOf<Message>()

    private var receiverName: String? = null
    private var receiverId: String? = null
    private var product: Product? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPesanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Retrieve chatId, receiverId, and receiverName from Intent
        chatId = intent.getStringExtra("chat_id")
        receiverId = intent.getStringExtra("receiver_id") // Receiver's ID
        receiverName = intent.getStringExtra("receiver_name") // Receiver's Name

        // Retrieve product info if available (i.e., product seller's username)
        product = intent.getParcelableExtra("product") // Ensure Product is passed

        // If product is available, set the receiverName to the product's userName (seller's name)
        if (product != null) {
            receiverName = product?.userName // Use the product's userName as receiverName
        }

        if (chatId == null) {
            Toast.makeText(this, "ID percakapan tidak ditemukan", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Pass the current user id to the adapter
        val currentUserId = auth.currentUser?.uid ?: ""

        // Initialize RecyclerView with ChatSendAdapter
        binding.rvMessages.layoutManager = LinearLayoutManager(this)
        messageAdapter = ChatSendAdapter(messageList, currentUserId)
        binding.rvMessages.adapter = messageAdapter

        // Load messages from Firestore
        loadMessages()

        // Send message when user clicks the send button
        binding.btnSend.setOnClickListener {
            val messageText = binding.etMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText, receiverId, receiverName)  // Send message to receiver
            } else {
                Toast.makeText(this, "Pesan tidak boleh kosong", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadMessages() {
        firestore.collection("chats")
            .document(chatId ?: "")
            .collection("messages")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    Log.e("PesanActivity", "Error loading messages: $exception")
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    messageList.clear()

                    for (document in snapshot.documents) {
                        val message = document.toObject(Message::class.java)
                        message?.let {
                            // Ensure receiverName is loaded correctly
                            if (it.receiverName.isEmpty()) {
                                it.receiverName = "Receiver"
                            }
                            messageList.add(it)
                        }
                    }

                    messageAdapter.notifyDataSetChanged()
                    binding.rvMessages.scrollToPosition(messageList.size - 1)  // Scroll to the latest message
                }
            }
    }


    private fun sendMessage(messageText: String, receiverId: String?, receiverName: String?) {
        val senderId = auth.currentUser?.uid ?: return
        val senderName = auth.currentUser?.displayName ?: "Unknown"

        // Pastikan receiverName sudah diterima dengan benar dari produk atau intent
        val actualReceiverName = receiverName ?: "Receiver"  // Gunakan receiverName dari Intent atau fallback ke "Receiver"

        // Pastikan receiverId valid
        val actualReceiverId = receiverId ?: ""

        // Membuat objek pesan dengan informasi pengirim dan penerima
        val message = Message(
            senderId = senderId,
            senderName = senderName,  // Nama pengirim
            message = messageText,
            timestamp = System.currentTimeMillis(),
            receiverId = actualReceiverId,  // ID penerima (dari produk atau yang disediakan)
            receiverName = actualReceiverName,  // Nama penerima
            chatId = chatId ?: "chat_${senderId}_${actualReceiverId}" // Menyusun chatId jika belum ada
        )

        // Menggunakan chatId yang valid atau membuatnya jika belum ada
        val currentChatId = chatId ?: "chat_${senderId}_${actualReceiverId}"

        // Cek apakah chat sudah ada di Firestore
        firestore.collection("chats").document(currentChatId).get()

            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    // Jika chat belum ada, buat chat baru dan simpan peserta serta receiverName
                    firestore.collection("chats").document(currentChatId).set(

                        hashMapOf(
                            "participants" to listOf(senderId, actualReceiverId),
                            "timestamp" to System.currentTimeMillis(),
                            "receiverName" to actualReceiverName  // Simpan receiverName pada dokumen chat
                        )
                    ).addOnSuccessListener {
                        // Setelah chat dibuat, tambahkan pesan pertama
                        firestore.collection("chats")
                            .document(currentChatId)
                            .collection("messages")
                            .add(message)  // Menambahkan pesan dengan chatId
                            .addOnSuccessListener {
                                Log.d("PesanActivity", "Message sent successfully")
                                binding.etMessage.text.clear()  // Bersihkan input setelah mengirim pesan
                            }
                            .addOnFailureListener { e ->
                                Log.e("PesanActivity", "Failed to send message: $e")
                            }
                    }
                } else {
                    // Jika chat sudah ada, langsung tambahkan pesan baru ke chat yang ada
                    firestore.collection("chats")
                        .document(currentChatId)
                        .collection("messages")
                        .add(message)  // Menambahkan pesan dengan chatId
                        .addOnSuccessListener {
                            Log.d("PesanActivity", "Message sent successfully")
                            binding.etMessage.text.clear()  // Bersihkan input setelah mengirim pesan
                        }
                        .addOnFailureListener { e ->
                            Log.e("PesanActivity", "Failed to send message: $e")
                        }
                }
            }
            .addOnFailureListener { e ->
                Log.e("PesanActivity", "Failed to check chat existence: $e")
            }
    }
}
