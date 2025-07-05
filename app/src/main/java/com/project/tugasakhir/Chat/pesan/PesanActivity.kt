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

        // Retrieve chatId, senderName, and receiverName from Intent
        chatId = intent.getStringExtra("chat_id")
        receiverId = intent.getStringExtra("receiver_id") // Receiver's ID
        receiverName = intent.getStringExtra("receiver_name") // Receiver's Name

        // Retrieve product info if available
        product = intent.getParcelableExtra("product") // Ensure Product is passed

        // If the product is available, set the receiverName to the product's userName
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

        // Send message when user clicks send button
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
        val message = Message(
            senderId = senderId,
            senderName = auth.currentUser?.displayName ?: "Unknown",
            message = messageText,
            timestamp = System.currentTimeMillis(),
            receiverId = receiverId ?: "",  // Receiver's ID
            receiverName = receiverName ?: "Receiver"  // Receiver's name (from Product if available)
        )

        // Send message to Firestore under the existing chatId
        firestore.collection("chats")
            .document(chatId ?: "")
            .collection("messages")
            .add(message)
            .addOnSuccessListener {
                Log.d("PesanActivity", "Message sent successfully")
                // After sending, clear the input field
                binding.etMessage.text.clear()
            }
            .addOnFailureListener { e ->
                Log.e("PesanActivity", "Failed to send message: $e")
            }
    }
}
