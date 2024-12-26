package com.example.attemptdmd

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*
import com.example.attemptdmd.R


class ChatAdapter(private val items: MutableList<ChatItem>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_MESSAGE = 1
        private const val VIEW_TYPE_LOADING = 2
    }

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

    override fun getItemViewType(position: Int) = when (items[position]) {
        is ChatItem.Message -> VIEW_TYPE_MESSAGE
        is ChatItem.Loading -> VIEW_TYPE_LOADING
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
        VIEW_TYPE_MESSAGE -> MessageViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false))
        else -> LoadingViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_loading, parent, false))
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is MessageViewHolder -> {
                val message = items[position] as ChatItem.Message
                if (message.isUser) {
                    holder.userContainer.visibility = View.VISIBLE
                    holder.assistantContainer.visibility = View.GONE
                    holder.tvUserMessage.text = message.text
                } else {
                    holder.userContainer.visibility = View.GONE
                    holder.assistantContainer.visibility = View.VISIBLE
                    holder.tvAssistantMessage.text = message.text
                    holder.tvAssistantDisclaimer.text = message.disclaimer
                }
            }
            is LoadingViewHolder -> holder.progressBar.visibility = View.VISIBLE
        }
    }

    fun addItem(item: ChatItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun removeItem(item: ChatItem) {
        val index = items.indexOf(item)
        if (index != -1) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}
