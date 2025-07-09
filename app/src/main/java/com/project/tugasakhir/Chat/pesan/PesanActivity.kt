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

        // Ensure receiverName is loaded correctly from the product or passed intent
        val actualReceiverName = receiverName ?: "Receiver"
        val actualReceiverId = receiverId ?: ""

        // Membuat objek pesan dengan informasi pengirim dan penerima
        val message = Message(
            senderId = senderId,
            senderName = senderName,  // Sender's name
            message = messageText,
            timestamp = System.currentTimeMillis(),
            receiverId = actualReceiverId,  // Receiver's ID from OrderNumber or elsewhere
            receiverName = actualReceiverName,  // Receiver's name
            chatId = chatId
                ?: "chat_${senderId}_${actualReceiverId}" // Use existing chatId or generate new
        )

        // Cek apakah chat sudah ada di Firestore
        firestore.collection("chats").document(chatId ?: "").get()
            .addOnSuccessListener { document ->
                if (!document.exists()) {
                    // If no existing chat, create a new chat and send the message
                    val newChatId = "chat_${senderId}_${actualReceiverId}"
                    createChatAndSendMessage(newChatId, message)
                } else {
                    // If chat exists, add the message to the existing chat
                    firestore.collection("chats")
                        .document(chatId ?: "")
                        .collection("messages")
                        .add(message)
                        .addOnSuccessListener {
                            Log.d("PesanActivity", "Message sent successfully")
                            binding.etMessage.text.clear()  // Clear input after sending
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

    private fun createChatAndSendMessage(chatId: String, message: Message) {
        val senderId = auth.currentUser?.uid ?: return
        val senderName = auth.currentUser?.displayName ?: "Unknown"

        // Create the chat document and add participants
        firestore.collection("chats").document(chatId)
            .set(
                hashMapOf(
                    "participants" to listOf(senderId, message.receiverId),
                    "timestamp" to System.currentTimeMillis(),
                    "receiverName" to message.receiverName
                )
            )
            .addOnSuccessListener {
                // After creating the chat, add the first message
                firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .add(message)
                    .addOnSuccessListener {
                        Log.d("PesanActivity", "Message sent successfully")
                        binding.etMessage.text.clear()  // Clear input after sending
                    }
                    .addOnFailureListener { e ->
                        Log.e("PesanActivity", "Failed to send message: $e")
                    }
            }
            .addOnFailureListener { e ->
                Log.e("PesanActivity", "Failed to create chat: $e")
            }
    }
}
