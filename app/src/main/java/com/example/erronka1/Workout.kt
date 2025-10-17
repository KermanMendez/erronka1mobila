package com.example.erronka1

data class Workout(
    var title: String = "",
    var description: String = "",
    var duration: Int = 0,
    var difficulty: Int = 0,
    var ariketak: List<Ariketa> = emptyList()

)
