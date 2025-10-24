package com.example.erronka1.rvWorkout

import android.content.res.ColorStateList
import android.graphics.Paint
import android.view.View
import android.widget.CheckBox
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.erronka1.R
import com.example.erronka1.model.Workout

class WorkoutViewHolder (view: View) : RecyclerView.ViewHolder(view) {
    val rbSelectWorkout: RadioButton = view.findViewById(R.id.rbSelectWorkout)
    private val tvNameWorkout: TextView = view.findViewById(R.id.tvNameWorkout)
    private val tvLevel: TextView = view.findViewById(R.id.tvLevel)
    private val tvTotalTime: TextView = view.findViewById(R.id.tvTotalTime)
    private val tvExpectedTime: TextView = view.findViewById(R.id.tvExpectedTime)
    private val tvDate: TextView = view.findViewById(R.id.tvDate)
    private val tvDoneExercisesPercent: TextView = view.findViewById(R.id.tvDoneExercisesPercent)


    fun render(workout: Workout) {

        tvNameWorkout.text = workout.title
        tvLevel.text = workout.level.toString()
        tvTotalTime.text = workout.description
        /*tvExpectedTime.text =
        tvDate.text =
        tvDoneExercisesPercent.text = */

    }
}