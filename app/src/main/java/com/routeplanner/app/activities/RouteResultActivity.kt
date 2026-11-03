package com.routeplanner.app.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.routeplanner.app.adapters.OptimizedRouteAdapter
import com.routeplanner.app.data.database.RouteDatabase
import com.routeplanner.app.data.database.entity.RouteStopEntity
import com.routeplanner.app.databinding.ActivityRouteResultBinding
import com.routeplanner.app.repository.DestinationRepository
import com.routeplanner.app.repository.RouteRepository
import kotlinx.coroutines.launch
import com.routeplanner.app.repository.RouteStopRepository
import kotlinx.coroutines.flow.collectLatest
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.flow.first

class RouteResultActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DESTINATION_IDS = "extra_destination_ids"
        const val EXTRA_ROUTE_ID = "extra_route_id"
    }

    private lateinit var binding: ActivityRouteResultBinding
    //private lateinit var adapter: OptimizedRouteAdapter
    private lateinit var pendingAdapter: OptimizedRouteAdapter
    private lateinit var completedAdapter: OptimizedRouteAdapter
    private lateinit var repository: DestinationRepository
    private lateinit var routeRepository: RouteRepository
    private lateinit var routeStopRepository: RouteStopRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRouteResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Configurar Toolbar
        setSupportActionBar(binding.toolbar)

        // Activar flecha de navegación
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        // Acción de la flecha
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        val database = RouteDatabase.getDatabase(applicationContext)

        repository = DestinationRepository(database.destinationDao())

        routeStopRepository = RouteStopRepository(database.routeStopDao())

        routeRepository = RouteRepository(
            routeDao = database.routeDao(),
            routeStopDao = database.routeStopDao(),
            database = database
        )

        binding.btnEditRoute.setOnClickListener {
            val routeId = intent.getIntExtra(EXTRA_ROUTE_ID, -1)

            if (routeId != -1) {
                val editIntent = Intent(
                    this,
                    EditRouteActivity::class.java
                )

                editIntent.putExtra(EXTRA_ROUTE_ID, routeId)
                startActivity(editIntent)
            }
        }

        binding.btnFinishRoute.setOnClickListener {
            confirmFinishRoute()
        }

        binding.btnPauseResumeRoute.setOnClickListener {
            togglePauseResumeRoute()
        }

        configureRecyclerView()
        loadRoute()
    }

    private fun togglePauseResumeRoute() {

        val routeId = intent.getIntExtra(EXTRA_ROUTE_ID, -1)

        if (routeId == -1) {
            Toast.makeText(
                this,
                "No se ha encontrado la ruta.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        lifecycleScope.launch {
            try {
                val route = routeRepository.getRouteById(routeId)

                if (route == null) {
                    Toast.makeText(
                        this@RouteResultActivity,
                        "No se ha encontrado la ruta.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                if (route.status == "ACTIVE") {
                    routeRepository.pauseRoute(routeId)

                    Toast.makeText(
                        this@RouteResultActivity,
                        "Ruta pausada.",
                        Toast.LENGTH_SHORT
                    ).show()

                } else if (route.status == "PAUSED") {
                    routeRepository.resumeRoute(routeId)

                    Toast.makeText(
                        this@RouteResultActivity,
                        "Ruta reanudada.",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                updatePauseResumeButton(
                    if (route.status == "ACTIVE") "PAUSED" else "ACTIVE"
                )

            } catch (e: Exception) {
                Toast.makeText(
                    this@RouteResultActivity,
                    "Error al cambiar el estado: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun updatePauseResumeButton(status: String) {

        binding.btnPauseResumeRoute.text = when (status) {
            "PAUSED" -> "Reanudar ruta"
            else -> "Pausar ruta"
        }
    }

    private fun confirmFinishRoute() {

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Finalizar ruta")
            .setMessage(
                "¿Estás seguro de que quieres finalizar esta ruta? " +
                        "Podrás consultarla posteriormente en el historial."
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Finalizar") { _, _ ->

                lifecycleScope.launch {
                    try {
                        val routeId = intent.getIntExtra(EXTRA_ROUTE_ID, -1)

                        if (routeId == -1) {
                            Toast.makeText(
                                this@RouteResultActivity,
                                "No se ha encontrado la ruta",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@launch
                        }

                        routeRepository.finishRoute(routeId)

                        Toast.makeText(
                            this@RouteResultActivity,
                            "Ruta finalizada correctamente",
                            Toast.LENGTH_SHORT
                        ).show()

                        finish()

                    } catch (e: Exception) {
                        Toast.makeText(
                            this@RouteResultActivity,
                            "Error al finalizar la ruta: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .show()
    }

    private fun configureRecyclerView() {

        // Adaptador de paradas pendientes
        pendingAdapter = OptimizedRouteAdapter(
            onCompleteStop = { stop ->
                completeStop(stop)
            },
            showCompleteButton = true,
            onNavigateToStop = { stop ->
                navigateToStop(stop)
            }
        )

        // Adaptador de paradas completadas
        completedAdapter = OptimizedRouteAdapter(
            onCompleteStop = { stop ->
                reopenStop(stop)
            },
            showCompleteButton = false,
            onNavigateToStop = { stop ->
                navigateToStop(stop)
            }
        )

        binding.rvPendingStops.layoutManager =
            LinearLayoutManager(this)

        binding.rvPendingStops.adapter = pendingAdapter

        binding.rvCompletedStops.layoutManager =
            LinearLayoutManager(this)

        binding.rvCompletedStops.adapter = completedAdapter
    }


    private fun navigateToStop(stop: RouteStopEntity) {

        val navigationApp = getSharedPreferences(
            "route_preferences",
            MODE_PRIVATE
        ).getString("navigation_app", "GOOGLE_MAPS")

        val latitude = stop.latitude
        val longitude = stop.longitude

        if (latitude == null || longitude == null) {
            Toast.makeText(
                this,
                "Esta parada no tiene coordenadas disponibles.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        try {
            if (navigationApp == "WAZE") {

                // Intentar abrir directamente la aplicación Waze
                val wazeIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("waze://?ll=$latitude,$longitude&navigate=yes")
                ).apply {
                    setPackage("com.waze")
                }

                if (wazeIntent.resolveActivity(packageManager) != null) {
                    startActivity(wazeIntent)
                } else {
                    // Si Waze no está instalado, abrir su versión web
                    val webIntent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse(
                            "https://waze.com/ul?ll=$latitude,$longitude&navigate=yes"
                        )
                    )
                    startActivity(webIntent)
                }

            } else {

                val googleIntent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("google.navigation:q=$latitude,$longitude")
                ).apply {
                    setPackage("com.google.android.apps.maps")
                }

                startActivity(googleIntent)
            }

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "No se ha podido abrir la aplicación de navegación.",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    private fun reopenStop(stop: RouteStopEntity) {

        lifecycleScope.launch {
            try {
                routeStopRepository.reopenStop(stop.id)

                Toast.makeText(
                    this@RouteResultActivity,
                    "Parada devuelta a pendientes.",
                    Toast.LENGTH_SHORT
                ).show()

            } catch (e: Exception) {

                Toast.makeText(
                    this@RouteResultActivity,
                    "Error al reabrir la parada: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun completeStop(stop: RouteStopEntity) {

        lifecycleScope.launch {
            try {
                routeStopRepository.completeStop(stop.id)

                Toast.makeText(
                    this@RouteResultActivity,
                    "Parada completada.",
                    Toast.LENGTH_SHORT
                ).show()

                checkIfAllStopsCompleted()

            } catch (e: Exception) {

                Toast.makeText(
                    this@RouteResultActivity,
                    "Error al completar la parada: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private suspend fun checkIfAllStopsCompleted() {

        val routeId = intent.getIntExtra(EXTRA_ROUTE_ID, -1)

        if (routeId == -1) return

        val stops = routeStopRepository
            .getStopsByRoute(routeId)
            .first()

        if (stops.isNotEmpty() && stops.all { it.isCompleted }) {
            showFinishRouteSuggestion()
        }
    }

    private fun showFinishRouteSuggestion() {

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("¡Has completado todas las paradas!")
            .setMessage(
                "Has terminado todos los destinos de esta ruta. " +
                        "¿Quieres finalizarla ahora?"
            )
            .setNegativeButton("Más tarde", null)
            .setPositiveButton("Finalizar ruta") { _, _ ->
                confirmFinishRoute()
            }
            .show()
    }

    private fun loadRoute() {

        val routeId = intent.getIntExtra(EXTRA_ROUTE_ID, -1)

        if (routeId == -1) {
            Toast.makeText(
                this,
                "No se ha recibido una ruta válida.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        lifecycleScope.launch {

            val route = routeRepository.getRouteById(routeId)

            if (route == null) {
                Toast.makeText(
                    this@RouteResultActivity,
                    "No se ha encontrado la ruta",
                    Toast.LENGTH_LONG
                ).show()

                finish()
                return@launch
            }

            binding.toolbar.title = route.name
            updatePauseResumeButton(route.status)

            routeStopRepository
                .getStopsByRoute(routeId)
                .collectLatest { stops ->

                    if (stops.isEmpty()) {
                        Toast.makeText(
                            this@RouteResultActivity,
                            "No se han podido recuperar las paradas.",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                        return@collectLatest
                    }

                    val pendingStops = stops.filter { !it.isCompleted }

                    val completedStops = stops
                        .filter { it.isCompleted }
                        .sortedBy { it.completedAt }

                    pendingAdapter.updateDestinations(pendingStops)
                    completedAdapter.updateDestinations(completedStops)

                    val completedCount = completedStops.size

                    binding.tvRouteSummary.text =
                        "${stops.size} paradas · $completedCount completadas"
                }
        }
    }
}