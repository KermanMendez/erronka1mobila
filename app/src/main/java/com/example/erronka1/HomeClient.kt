package com.example.erronka1

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.erronka1.databinding.ActivityHomeClientBinding
import com.example.erronka1.databinding.ActivityUserProfileBinding

class HomeClient : AppCompatActivity() {

    private lateinit var binding: ActivityHomeClientBinding

    private var language = listOf("Español", "Euskara", "English")
    private var selectedLanguageChoice: String = language[0]

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
            print("kaka")
            if (up) {
                binding.ivUpDownArrow.setImageResource(R.drawable.outline_keyboard_arrow_down_24)
                up = false
            } else {
                binding.ivUpDownArrow.setImageResource(R.drawable.outline_keyboard_arrow_up_24)
                up = true
            }
        }
        binding.ivProfile.setOnClickListener {
            showUserProfileDialog()

        }
    }
    private fun showUserProfileDialog() {

        val userBinding = ActivityUserProfileBinding.inflate(layoutInflater)

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, language)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        userBinding.spLanguages.adapter = adapter
        userBinding.spLanguages.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedLanguageChoice = language[position]
            }
            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        val dialog = Dialog(this)
        dialog.setContentView(userBinding.root)
        dialog.show()
    }
}