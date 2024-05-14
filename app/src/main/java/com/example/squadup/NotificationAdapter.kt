package com.example.squadup

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class NotificationAdapter(private val notificationList: List<Notification>) :
    RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    class NotificationViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleTextView: TextView = itemView.findViewById(R.id.notification_title)
        val messageTextView: TextView = itemView.findViewById(R.id.notification_message)
        val timestampTextView: TextView = itemView.findViewById(R.id.notification_timestamp)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val itemView = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_notification, parent, false)
        return NotificationViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        val currentItem = notificationList[position]
        holder.titleTextView.text = currentItem.title
        holder.messageTextView.text = currentItem.message
        holder.timestampTextView.text = currentItem.timestamp.toString() // Format the timestamp as needed
    }

    override fun getItemCount() = notificationList.size
}
