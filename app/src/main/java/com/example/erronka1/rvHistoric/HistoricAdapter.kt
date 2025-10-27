package com.example.erronka1.rvHistoric

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.erronka1.R
import com.example.erronka1.model.Workout

class HistoricAdapter (private val workouts: List<Workout>, private val onWorkoutSelected: (Int) -> Unit) :
    RecyclerView.Adapter<HistoricViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoricViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_workout, parent, false)
        return HistoricViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoricViewHolder, position: Int) {
        holder.render(workouts[position])
        holder.itemView.setOnClickListener {
            onWorkoutSelected(position)
        }
    }

    override fun getItemCount(): Int {
        return workouts.size
    }
}