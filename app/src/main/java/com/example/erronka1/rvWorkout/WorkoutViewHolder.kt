package com.example.erronka1.rvWorkout

import android.content.res.ColorStateList
import android.graphics.Paint
import android.view.View
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.erronka1.R
import com.example.erronka1.model.Workout

class WorkoutViewHolder (view: View) : RecyclerView.ViewHolder(view) {

    private val tvWorkoutTitle: TextView = view.findViewById(R.id.tvWorkoutTitle)
    private val workoutSelectionCard: CardView = view.findViewById(R.id.workoutSelectionCard)
    fun render(workout: Workout, onItemSelected: (Int) -> Unit) {

        tvWorkoutTitle.text = workout.title

        val color = if (workout.isSelected) {
            R.color.background_workout
        } else {
            R.color.background_disabled
        }

        workoutSelectionCard.setCardBackgroundColor(ContextCompat.getColor(workoutSelectionCard.context, color))
        itemView.setOnClickListener { onItemSelected(layoutPosition) }
    }
}