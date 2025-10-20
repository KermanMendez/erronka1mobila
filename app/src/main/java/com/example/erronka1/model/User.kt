package com.example.erronka1.model

data class User(
    var id: String = "",
    var name: String = "",
    var surname: String = "",
    var surname2: String = "",
    var birthdate: String = "",
    var isTrainer: Boolean = false,
    var level: Number = 0,
    // Need to se if it's necessary to initialize workouts to empty list or to be 2 separated objets
    var workouts: List<Workout> = emptyList()
)

