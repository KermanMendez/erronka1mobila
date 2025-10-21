package com.example.erronka1.rvLevel


import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.erronka1.R
import com.example.erronka1.modelo.Level

class LevelAdapter(private val levels: List<Level>, private val onItemSelected:(Int) -> Unit) :
    RecyclerView.Adapter<LevelViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LevelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_level, parent, false)
        return LevelViewHolder(view)
    }

    override fun onBindViewHolder(holder: LevelViewHolder, position: Int) {
        holder.render(levels[position], onItemSelected)
    }

    override fun getItemCount(): Int {
        return levels.size
    }

}