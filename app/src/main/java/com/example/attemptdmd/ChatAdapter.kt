package com.example.kotlin_dmd_pro

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class ChatAdapter(val items: MutableList<ChatItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_MESSAGE = 1
        private const val VIEW_TYPE_LOADING = 2
    }

    // ViewHolders
    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val assistantContainer: View = itemView.findViewById(R.id.assistantMessageContainer)
        val tvAssistantMessage: TextView = itemView.findViewById(R.id.tvAssistantMessage)
        val tvAssistantDisclaimer: TextView = itemView.findViewById(R.id.tvAssistantDisclaimer)
        val userContainer: View = itemView.findViewById(R.id.userMessageContainer)
        val tvUserMessage: TextView = itemView.findViewById(R.id.tvUserMessage)
    }

    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val progressBar: View = itemView.findViewById(R.id.progressBar)
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is ChatItem.Message -> VIEW_TYPE_MESSAGE
            is ChatItem.Loading -> VIEW_TYPE_LOADING
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == VIEW_TYPE_MESSAGE) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
            return MessageViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_loading, parent, false)
            return LoadingViewHolder(view)
        }
    }

    override fun getItemCount(): Int = items.size

    // Define a CoroutineScope with SupervisorJob to manage coroutines
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is MessageViewHolder) {
            val messageItem = items[position] as ChatItem.Message
            if (messageItem.isUserMessage) {
                holder.userContainer.visibility = View.VISIBLE
                holder.assistantContainer.visibility = View.GONE
                holder.tvUserMessage.text = messageItem.text
            } else {
                holder.assistantContainer.visibility = View.VISIBLE
                holder.userContainer.visibility = View.GONE
                // Implement typewriter effect using the defined CoroutineScope
                holder.tvAssistantMessage.text = ""
                holder.tvAssistantDisclaimer.text = messageItem.disclaimer
                typeWriterEffect(holder.tvAssistantMessage, messageItem.text)
            }
        } else if (holder is LoadingViewHolder) {
            // Show loading indicator (e.g., ProgressBar)
            holder.progressBar.visibility = View.VISIBLE
        }
    }

    // Method to add a single ChatItem
    fun addItem(item: ChatItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    // Method to remove a specific ChatItem
    fun removeItem(item: ChatItem) {
        val index = items.indexOf(item)
        if (index != -1) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    // Typewriter effect implementation using Coroutines
    private fun typeWriterEffect(textView: TextView, text: String) {
        // Cancel any existing job associated with this TextView
        (textView.tag as? Job)?.cancel()

        // Launch a new coroutine for the typewriter effect
        val job = scope.launch {
            textView.text = ""
            for (char in text) {
                textView.append(char.toString())
                delay(5) // milliseconds per character
            }
        }

        // Associate the coroutine job with the TextView to manage its lifecycle
        textView.tag = job
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        scope.cancel() // Cancel all coroutines when the adapter is detached
    }
}
