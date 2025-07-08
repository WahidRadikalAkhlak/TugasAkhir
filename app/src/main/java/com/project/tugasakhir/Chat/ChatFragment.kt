package com.project.tugasakhir.Chat

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Adapter.ChatHistoryAdapter
import com.project.tugasakhir.Chat.pesan.PesanActivity
import com.project.tugasakhir.Data.Message
import com.project.tugasakhir.databinding.FragmentChatBinding

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding
        get() = _binding ?: throw IllegalStateException("Binding must be initialized before use!")

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var chatHistoryAdapter: ChatHistoryAdapter  // Adapter untuk chat history
    private val chatList = mutableListOf<Message>()  // Daftar chat yang akan ditampilkan

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set up RecyclerView and Adapter for displaying chat history
        chatHistoryAdapter = ChatHistoryAdapter(chatList, auth.currentUser?.uid ?: "") { chat ->
            openChatDetail(chat)  // Open chat detail when clicked
        }
        binding.rvPesan.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPesan.adapter = chatHistoryAdapter

        loadChatMessages() // Load chat messages from Firestore
    }

    private fun loadChatMessages() {
        val currentUserId = auth.currentUser?.uid ?: return

        // Fetch chats where the current user is a participant
        db.collection("chats")
            .whereArrayContains("participants", currentUserId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    showToast("No chats found")
                } else {
                    querySnapshot.documents.forEach { document ->
                        val chatId = document.id
                        val participants = document.get("participants") as List<String>
                        val timestamp = document.getLong("timestamp") ?: 0L

                        // Find the receiver's ID (the other participant)
                        val otherParticipant = participants.find { it != currentUserId }
                        if (otherParticipant != null) {
                            // Fetch receiver's name from Firestore using receiverId
                            db.collection("users")
                                .document(otherParticipant)
                                .get()
                                .addOnSuccessListener { receiverDoc ->
                                    val receiverName = receiverDoc.getString("nama") ?: "Receiver"

                                    // Create a Message object with the correct receiver name
                                    val chat = Message(
                                        senderId = "",  // Not required for chat listing
                                        senderName = "",  // Not required for chat listing
                                        message = "Click to chat",  // Placeholder message
                                        timestamp = timestamp,
                                        participants = participants,
                                        receiverId = otherParticipant,  // Correct receiverId
                                        receiverName = receiverName,  // Correct receiverName
                                        chatId = chatId
                                    )
                                    chatList.add(chat)
                                    chatHistoryAdapter.notifyDataSetChanged()  // Notify adapter to update UI
                                }
                                .addOnFailureListener { e ->
                                    showToast("Failed to load receiver's name: ${e.message}")
                                }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                showToast("Failed to load chats: ${e.message}")
            }
    }

    private fun openChatDetail(chat: Message) {
        val intent = Intent(requireContext(), PesanActivity::class.java)
        intent.putExtra("chat_id", chat.chatId)

        // Find the other participant in the chat
        val otherParticipant = chat.participants.find { it != auth.currentUser?.uid }
        if (otherParticipant != null) {
            intent.putExtra("receiver_id", otherParticipant)  // Pass receiverId to PesanActivity
        }

        startActivity(intent)  // Open PesanActivity to send messages
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}

