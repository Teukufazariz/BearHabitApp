package com.example.bearhabitapp.Adapter

import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.bearhabitapp.Model.Message
import com.example.bearhabitapp.R
import java.util.*

class MessageAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val messages = mutableListOf<Message>()

    // View Types untuk membedakan posisi pesan
    private val VIEW_TYPE_TEXT_SENDER = 1
    private val VIEW_TYPE_TEXT_RECEIVER = 2
    private val VIEW_TYPE_IMAGE_SENDER = 3
    private val VIEW_TYPE_IMAGE_RECEIVER = 4

    fun submitList(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return when {
            message.type == "image" && message.isSender -> VIEW_TYPE_IMAGE_SENDER
            message.type == "image" && !message.isSender -> VIEW_TYPE_IMAGE_RECEIVER
            message.isSender -> VIEW_TYPE_TEXT_SENDER
            else -> VIEW_TYPE_TEXT_RECEIVER
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_IMAGE_SENDER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_image_sender, parent, false)
                ImageMessageViewHolder(view)
            }
            VIEW_TYPE_IMAGE_RECEIVER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_image_receiver, parent, false)
                ImageMessageViewHolder(view)
            }
            VIEW_TYPE_TEXT_SENDER -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_text_sender, parent, false)
                TextMessageViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_text_receiver, parent, false)
                TextMessageViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        when (holder) {
            is TextMessageViewHolder -> holder.bind(message)
            is ImageMessageViewHolder -> holder.bind(message)
        }
    }

    override fun getItemCount(): Int = messages.size

    // ViewHolder untuk pesan teks
    inner class TextMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvMessage: TextView = itemView.findViewById(R.id.tvMessage)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)

        fun bind(message: Message) {
            tvMessage.text = message.text
            tvTimestamp.text = formatTimestamp(message.timestamp)
        }
    }

    // ViewHolder untuk pesan gambar
    inner class ImageMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivMessage: ImageView = itemView.findViewById(R.id.ivMessage)
        private val tvTimestamp: TextView = itemView.findViewById(R.id.tvTimestamp)

        fun bind(message: Message) {
            tvTimestamp.text = formatTimestamp(message.timestamp)
            message.imageUrl?.let { url ->
                Glide.with(itemView.context)
                    .load(url)
                    .into(ivMessage)
            }
        }
    }

    private fun formatTimestamp(timestamp: Long): String {
        val calendar = Calendar.getInstance(Locale.getDefault())
        calendar.timeInMillis = timestamp
        return DateFormat.format("HH:mm", calendar).toString()
    }
}
