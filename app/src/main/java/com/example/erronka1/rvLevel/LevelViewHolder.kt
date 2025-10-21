package com.example.erronka1.rvLevel

import android.view.View
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.erronka1.R
import com.example.erronka1.modelo.Level

class LevelViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    private val tvLevel: TextView = view.findViewById(R.id.tvLevel)
    private val viewContainer: CardView = view.findViewById(R.id.viewContainer)

    fun render(level: Level, onItemSelected: (Int) -> Unit) {

        tvLevel.text = level.level.toString()+". "
    }
}