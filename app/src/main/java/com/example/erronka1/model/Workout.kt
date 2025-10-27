package com.example.erronka1.model

import com.google.firebase.firestore.Exclude

data class Workout(
    @get:Exclude var id: String = "",
    var title: String = "",
    var description: String = "",
    var level: Int = 0,
    var video: String = "",
    @get:Exclude var isSelected: Boolean = false,
    @get:Exclude var ariketak: List<Ariketa> = emptyList()
)
