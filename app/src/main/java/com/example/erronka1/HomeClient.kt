package com.example.erronka1

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.erronka1.databinding.ActivityHomeClientBinding

class HomeClient : AppCompatActivity() {

    private lateinit var binding: ActivityHomeClientBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeClientBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        var up = false
        binding.ivBacktoLogin.setOnClickListener {
            val intent = android.content.Intent(this, Login::class.java)
            startActivity(intent)
        }
        binding.llOrder.setOnClickListener {
            if (up) {
                binding.ivUpDownArrow.setImageResource(R.drawable.outline_keyboard_arrow_down_24)
                up = false
            } else {
                binding.ivUpDownArrow.setImageResource(R.drawable.outline_keyboard_arrow_up_24)
                up = true
            }
        binding.ivProfile.setOnClickListener {

        }
        }
    }
}