package com.example.erronka1

import android.annotation.SuppressLint
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.firestore

object FirebaseSingleton {

    val auth = FirebaseAuth.getInstance()
    @SuppressLint("StaticFieldLeak")
    val db = Firebase.firestore
}