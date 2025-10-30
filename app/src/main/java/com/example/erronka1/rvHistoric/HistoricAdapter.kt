package com.example.erronka1.rvHistoric

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.erronka1.R
import com.example.erronka1.model.Historic

class HistoricAdapter (private val historics: List<Historic>, private val onHistoricSelected: (Int) -> Unit) :
    RecyclerView.Adapter<HistoricViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoricViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_historic, parent, false)
        return HistoricViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoricViewHolder, position: Int) {
        holder.render(historics[position])
        holder.itemView.setOnClickListener {
            onHistoricSelected(position)
        }
    }

    override fun getItemCount(): Int {
        return historics.size
    }
}