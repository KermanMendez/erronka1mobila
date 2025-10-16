package com.example.erronka1

import android.app.Dialog
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.erronka1.databinding.LoginBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding : LoginBinding;

    private var language = listOf("Español","Euskara","English");

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        //setContentView(R.layout.login)

        binding = LoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        //initSpinner()

        binding.btnLogin.setOnClickListener { view: View ->
            val dialog = Dialog(this)
            dialog.setContentView(R.layout.user_profile)
            dialog.show()
        }
    }
    fun initSpinner() {
        val spinner = findViewById<Spinner>(R.id.spLanguages)

        val adapter = ArrayAdapter(this,android.R.layout.simple_spinner_item,language)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinner.adapter = adapter
        spinner.onItemSelectedListener = object :AdapterView.OnItemSelectedListener{
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selectedLanguage = language[position]
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }
    }
}