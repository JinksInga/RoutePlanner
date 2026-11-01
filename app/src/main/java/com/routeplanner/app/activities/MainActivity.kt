package com.routeplanner.app.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.routeplanner.app.databinding.ActivityMainBinding
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.routeplanner.app.data.database.RouteDatabase
import com.routeplanner.app.repository.RouteRepository
import kotlinx.coroutines.launch

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

            // Limpiar las direcciones seleccionadas de la ruta anterior
            getSharedPreferences("route_preferences",
                MODE_PRIVATE).edit()
                .remove("selected_destination_ids")
                .apply()

            val intent = Intent(
                this,
                NewRouteActivity::class.java
            )

            startActivity(intent)
        }

        binding.btnContinueRoute.setOnClickListener {
            continueRoute()
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

    private fun continueRoute() {
        lifecycleScope.launch {
            try {
                val database = RouteDatabase.getDatabase(applicationContext)

                val routeRepository = RouteRepository(
                    routeDao = database.routeDao(),
                    routeStopDao = database.routeStopDao(),
                    database = database
                )

                val currentRoute = routeRepository.getCurrentRoute()

                if (currentRoute == null) {
                    Toast.makeText(
                        this@MainActivity,
                        "No hay ninguna ruta activa o pausada",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    val intent = Intent(
                        this@MainActivity,
                        RouteResultActivity::class.java
                    )

                    intent.putExtra(
                        RouteResultActivity.EXTRA_ROUTE_ID,
                        currentRoute.id
                    )

                    startActivity(intent)
                }

            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Error al recuperar la ruta: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
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