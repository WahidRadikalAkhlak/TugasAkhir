package com.project.tugasakhir.Chat.pesan

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Adapter.ChatAdapter
import com.project.tugasakhir.Data.Message
import com.project.tugasakhir.databinding.ActivityPesanBinding

class PesanActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPesanBinding
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: ChatAdapter
    private var messages = mutableListOf<Message>()

    private var chatId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPesanBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get chatId passed from InfoProductActivity
        chatId = intent.getStringExtra("chat_id")

        if (chatId == null) {
            Toast.makeText(this, "Chat ID tidak tersedia", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Setup RecyclerView
        binding.rvMessages.layoutManager = LinearLayoutManager(this)

        // Pass the empty listener or provide the logic you want to handle on item click
        adapter = ChatAdapter(messages) { message ->
            // Handle message click if necessary
            Toast.makeText(this, "Clicked: ${message.message}", Toast.LENGTH_SHORT).show()
        }

        binding.rvMessages.adapter = adapter

        loadMessages()

        binding.btnSend.setOnClickListener {
            val messageText = binding.etMessage.text.toString().trim()
            if (messageText.isNotEmpty()) {
                sendMessage(messageText)
            } else {
                Toast.makeText(this, "Message cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadMessages() {
        val chatId = intent.getStringExtra("chat_id") ?: return

        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("timestamp")  // Order messages by timestamp
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Toast.makeText(this, "Error loading messages: ${error.message}", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                messages.clear()
                snapshot?.forEach { document ->
                    val message = document.toObject(Message::class.java)
                    messages.add(message)
                }
                adapter.notifyDataSetChanged()  // Notify the adapter to update the RecyclerView
            }
    }


    private fun sendMessage(messageText: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val senderName = auth.currentUser?.displayName ?: "Unknown"

        val message = Message(
            senderId = currentUserId,
            senderName = senderName,
            message = messageText,
            timestamp = System.currentTimeMillis()
        )

        // Assuming chatId is passed from previous activity
        val chatId = intent.getStringExtra("chat_id") ?: return

        // Storing the message under the correct chat document
        db.collection("chats")
            .document(chatId)
            .collection("messages")
            .add(message)
            .addOnSuccessListener {
                binding.etMessage.text.clear()  // Clear the input field after sending the message
                loadMessages()  // Reload messages
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to send message: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}
