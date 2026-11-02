package com.routeplanner.app.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.routeplanner.app.databinding.ActivityMainBinding
import android.content.Intent
import androidx.lifecycle.lifecycleScope
import com.routeplanner.app.data.database.RouteDatabase
import com.routeplanner.app.repository.RouteRepository
import kotlinx.coroutines.flow.first
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
            val intent = Intent(
                this,
                HistoryActivity::class.java
            )
            startActivity(intent)
        }

        binding.btnSettings.setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
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

                val currentRoutes = routeRepository
                    .getCurrentRoutes()
                    .first()

                if (currentRoutes.isEmpty()) {
                    Toast.makeText(
                        this@MainActivity,
                        "No hay rutas pendientes de finalizar.",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@launch
                }

                val intent = Intent(
                    this@MainActivity,
                    SelectRouteActivity::class.java
                )

                startActivity(intent)

            } catch (e: Exception) {
                Toast.makeText(
                    this@MainActivity,
                    "Error al cargar las rutas: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}