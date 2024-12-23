package com.arcrun.eo.Adapter

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.arcrun.eo.Activity.DetailEventActivity
import com.arcrun.eo.Activity.UpdateEventActivity
import com.arcrun.eo.Models.EventModels
import com.arcrun.eo.R
import com.arcrun.eo.databinding.ViewholderEventBinding
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EventAdapter(
    private val items: ArrayList<EventModels>,
    private val eventIds: List<String>,
    private val context: Context,
    private val onDelete: (String, Int) -> Unit
) : RecyclerView.Adapter<EventAdapter.Viewholder>() {

    inner class Viewholder(private val binding: ViewholderEventBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: EventModels, eventId: String, position: Int) {
            binding.namaEventTxt.text = item.nama_event
            binding.statusEventTxt.text = item.status_event!!.capitalize()
            binding.hargaTxt.text = "Rp.${item.harga}"
            binding.batasAkhirTxt.text = item.batas_akhir

            Glide.with(context)
                .load(item.gambar)
                .error(R.drawable.imagenotfound)
                .into(binding.gambarBg)

            binding.root.setOnClickListener {
                val intent = Intent(context, DetailEventActivity::class.java)
                intent.putExtra("event_name", item.nama_event)
                intent.putExtra("event_status", item.status_event)
                intent.putExtra("event_image", item.gambar)
                intent.putExtra("event_desc", item.deskripsi)
                intent.putExtra("event_price", item.harga)
                intent.putExtra("event_benefit", item.benefit)
                intent.putExtra("batas_akhir", item.batas_akhir)
                intent.putExtra("waktu_mulai", item.waktu_mulai)
                intent.putExtra("kuota", item.kuota)
                intent.putExtra("event_id", eventId)
                context.startActivity(intent)
            }

            binding.updateEventTxt.setOnClickListener {
                val intent = Intent(context, UpdateEventActivity::class.java)
                intent.putExtra("object", item)
                intent.putExtra("eventId", eventId)
                context.startActivity(intent)
            }

            binding.deleteEventTxt.setOnClickListener {
                onDelete(eventId, position)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Viewholder {
        val binding = ViewholderEventBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return Viewholder(binding)
    }

    override fun onBindViewHolder(holder: Viewholder, position: Int) {
        holder.bind(items[position], eventIds[position], position)
    }

    override fun getItemCount(): Int = items.size
}