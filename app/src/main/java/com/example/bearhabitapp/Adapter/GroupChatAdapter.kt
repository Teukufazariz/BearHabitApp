package com.example.bearhabitapp.Adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.bearhabitapp.Model.GroupChat
import com.example.bearhabitapp.R

class GroupChatAdapter(private val onGroupChatClick: (GroupChat) -> Unit) :
    RecyclerView.Adapter<GroupChatAdapter.GroupChatViewHolder>() {

    private val groupChats = mutableListOf<GroupChat>()

    fun submitList(newGroupChats: List<GroupChat>) {
        groupChats.clear()
        groupChats.addAll(newGroupChats)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_group_chat, parent, false)
        return GroupChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupChatViewHolder, position: Int) {
        val groupChat = groupChats[position]
        holder.bind(groupChat)
        holder.itemView.setOnClickListener { onGroupChatClick(groupChat) }
    }

    override fun getItemCount(): Int = groupChats.size

    class GroupChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvGroupName: TextView = itemView.findViewById(R.id.tvGroupName)
        private val tvGroupMembers: TextView = itemView.findViewById(R.id.tvGroupMembers) // Tambahkan reference ke TextView untuk anggota

        fun bind(groupChat: GroupChat) {
            tvGroupName.text = groupChat.habitName // Tampilkan nama habit
            // Tampilkan daftar anggota sebagai string, misalnya email mereka
            tvGroupMembers.text = "Members: ${groupChat.members.joinToString(", ")}"
        }
    }

}
