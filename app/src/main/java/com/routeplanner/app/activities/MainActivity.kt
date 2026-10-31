package com.routeplanner.app.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.routeplanner.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity()
{
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configureButtons()
        /*enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }*/
    }

    private fun configureButtons()
    {
        binding.btnNewRoute.setOnClickListener {
            val intent = android.content.Intent(
                this,
                NewRouteActivity::class.java
            )
            startActivity(intent)
        }

        binding.btnDestinations.setOnClickListener {
            val intent = android.content.Intent(
                this,
                DestinationsActivity::class.java
            )
            startActivity(intent)
        }

        binding.btnHistory.setOnClickListener {
            showMessage("Historial de rutas")
        }

        binding.btnSettings.setOnClickListener {
            showMessage("Configuración")
        }
    }

    private fun showMessage(message: String)
    {
        Toast.makeText(
            this,
            "$message - Próximamente",
            Toast.LENGTH_SHORT
        ).show()
    }
}