package com.arcrun.eo.Adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.arcrun.eo.Models.PesertaModels
import com.arcrun.eo.databinding.ViewholderDataPesertaBinding

class PesertaAdapter(private val pesertaList: List<PesertaModels>) :
    RecyclerView.Adapter<PesertaAdapter.ViewHolder>() {

    inner class ViewHolder(private val binding: ViewholderDataPesertaBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(peserta: PesertaModels) {
            binding.namaTxt.text = peserta.name
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ViewholderDataPesertaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(pesertaList[position])
    }

    override fun getItemCount(): Int = pesertaList.size
}
