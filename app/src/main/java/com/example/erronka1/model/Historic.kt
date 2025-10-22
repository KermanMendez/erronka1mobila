package com.example.erronka1.model

data class Historic(
    var id: String = "",
    var workoutId: String = "",
    var workoutTitle: String = "",
    var date: String = "", // kept as String per requirement
    var totalTime: Int = 0, // Duration in seconds
    var totalReps: Int = 0,
    var completed: Boolean = false
)
