package com.project.tugasakhir.Chat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.tugasakhir.Adapter.ChatAdapter
import com.project.tugasakhir.Chat.pesan.PesanActivity
import com.project.tugasakhir.Data.Message
import com.project.tugasakhir.databinding.FragmentChatBinding

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding harus diinisialisasi sebelum dipakai!")

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var adapter: ChatAdapter
    private val chatMessages = mutableListOf<Message>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize RecyclerView with ChatAdapter
        binding.rvPesan.layoutManager = LinearLayoutManager(requireContext())
        adapter = ChatAdapter(chatMessages) { message -> openChatDetail(message) }
        binding.rvPesan.adapter = adapter

        // Load chat messages from Firestore
        loadChatMessages()
    }

    private fun loadChatMessages() {
        val currentUserId = auth.currentUser?.uid ?: return

        // Debugging: Check if the currentUserId is being fetched correctly
        Log.d("ChatFragment", "Current User ID: $currentUserId")

        db.collection("chats")
            .whereArrayContains("participants", currentUserId)  // Look for chats where the current user is a participant
            .get()  // Using .get() instead of .addSnapshotListener for a one-time query
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Toast.makeText(requireContext(), "No chats found", Toast.LENGTH_SHORT).show()
                } else {
                    // Process the documents in the snapshot
                    querySnapshot.documents.forEach { document ->
                        val chatId = document.id
                        Log.d("ChatFragment", "Chat ID found: $chatId")
                        loadMessagesForChat(chatId)  // Load messages for this chat
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Failed to load chats: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadMessagesForChat(chatId: String) {
        db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp")  // Sort by timestamp to get messages in order
            .get()  // Using .get() for a one-time fetch
            .addOnSuccessListener { snapshot ->
                chatMessages.clear()
                snapshot.documents.forEach { document ->
                    val message = document.toObject(Message::class.java)
                    message?.let {  // Use let to ensure message is not null
                        chatMessages.add(it)
                    }
                }
                adapter.notifyDataSetChanged()  // Notify adapter that data has changed
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error loading messages: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    // Open chat details when a chat item is clicked
    private fun openChatDetail(message: Message) {
        val intent = Intent(requireContext(), PesanActivity::class.java)
        intent.putExtra("message_data", message)
        startActivity(intent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
