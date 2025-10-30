package com.example.erronka1.rvHistoric

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.erronka1.R
import com.example.erronka1.model.Historic

class HistoricViewHolder (view: View) : RecyclerView.ViewHolder(view) {

    private val tvWorkoutTitle: TextView = view.findViewById(R.id.tvHistoricTitle)
    private val tvDate: TextView = view.findViewById(R.id.tvDate)
    private val tvTotalTime: TextView = view.findViewById(R.id.tvTotalTime)
    private val tvTotalReps: TextView = view.findViewById(R.id.tvTotalReps)
    private val tvCompleted: TextView = view.findViewById(R.id.tvCompleted)
    fun render(historic: Historic) {

        tvWorkoutTitle.text = "Tituloa: "+historic.workoutTitle
        tvDate.text = "Deskripzioa: "+historic.date
        tvTotalTime.text = "Denbora totala: "+historic.totalTime.toString()
        tvTotalReps.text = "Errepikapen batura"+historic.totalReps.toString()
        if (historic.completed) {
            tvCompleted.text = "Historikoa osatua dago"
        } else {
            tvCompleted.text = "Historikoa osatu gabe dago"
        }

    }
}