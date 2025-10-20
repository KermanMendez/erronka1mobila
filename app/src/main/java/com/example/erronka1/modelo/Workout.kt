package com.example.erronka1.modelo

import com.google.firebase.firestore.Exclude

data class Workout(
    var title: String = "",
    var description: String = "",
    var level: Int = 0,
    @get:Exclude var ariketak: List<Ariketa> = emptyList()

)