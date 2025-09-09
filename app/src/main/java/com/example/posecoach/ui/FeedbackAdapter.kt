package com.example.posecoach.ui

import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.posecoach.databinding.ItemFeedbackBinding

class FeedbackAdapter(private val items: List<String>) :
    RecyclerView.Adapter<FeedbackAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemFeedbackBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(text: String) {
            binding.tvFeedbackItem.text = text
            when {
                text.contains("좋습니다") || text.contains("안정적") -> {
                    binding.tvFeedbackItem.setTextColor(Color.parseColor("#2E7D32")) // Green
                }
                text.contains("주의") || text.contains("조심") -> {
                    binding.tvFeedbackItem.setTextColor(Color.parseColor("#F9A825")) // Yellow
                }
                else -> {
                    binding.tvFeedbackItem.setTextColor(Color.parseColor("#C62828")) // Red
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding =
            ItemFeedbackBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}
