package com.example.erronka1.rvWorkout

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.erronka1.R
import com.example.erronka1.model.Workout

class WorkoutAdapter (private val workouts: List<Workout>, private val onWorkoutSelected: (Int) -> Unit) :
    RecyclerView.Adapter<WorkoutViewHolder>(){

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WorkoutViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_workout, parent, false)
        return WorkoutViewHolder(view)
    }

    private var selectedPosition = -1

    override fun onBindViewHolder(holder: WorkoutViewHolder, position: Int) {
        /*holder.render(workouts[position])
        holder.rbSelectWorkout.isChecked = position == selectedPosition

        holder.itemView.setOnClickListener {
            selectedPosition = holder.adapterPosition
            notifyDataSetChanged()
            onWorkoutSelected(selectedPosition)
        }*/
        holder.render(workouts[position])
        holder.itemView.setOnClickListener {
            onWorkoutSelected(position)
        }
    }

    override fun getItemCount(): Int {
        return workouts.size
    }
}