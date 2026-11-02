package com.routeplanner.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.routeplanner.app.adapters.RouteSelectionItem
import com.routeplanner.app.adapters.SelectRouteAdapter
import com.routeplanner.app.data.database.RouteDatabase
import com.routeplanner.app.data.database.entity.RouteEntity
import com.routeplanner.app.databinding.ActivitySelectRouteBinding
import com.routeplanner.app.repository.RouteRepository
import com.routeplanner.app.repository.RouteStopRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SelectRouteActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectRouteBinding
    private lateinit var routeRepository: RouteRepository
    private lateinit var routeStopRepository: RouteStopRepository
    private lateinit var adapter: SelectRouteAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySelectRouteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = RouteDatabase.getDatabase(applicationContext)

        routeRepository = RouteRepository(
            routeDao = database.routeDao(),
            routeStopDao = database.routeStopDao(),
            database = database
        )

        routeStopRepository = RouteStopRepository(database.routeStopDao())

        configureRecyclerView()

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        loadRoutes()
    }

    private fun configureRecyclerView() {
        adapter = SelectRouteAdapter { route ->
            openRoute(route)
        }

        binding.rvCurrentRoutes.layoutManager = LinearLayoutManager(this)
        binding.rvCurrentRoutes.adapter = adapter
    }

    private fun loadRoutes() {
        lifecycleScope.launch {
            try {
                val currentRoutes = routeRepository.getCurrentRoutes().first()

                if (currentRoutes.isEmpty()) {
                    binding.tvEmptyRoutes.visibility = View.VISIBLE
                    binding.rvCurrentRoutes.visibility = View.GONE
                    return@launch
                }

                val routeItems = currentRoutes.map { route ->
                    val stops = routeStopRepository
                        .getStopsByRoute(route.id)
                        .first()

                    RouteSelectionItem(
                        route = route,
                        totalStops = stops.size,
                        completedStops = stops.count { it.isCompleted }
                    )
                }

                adapter.updateRoutes(routeItems)

                binding.tvEmptyRoutes.visibility = View.GONE
                binding.rvCurrentRoutes.visibility = View.VISIBLE

            } catch (e: Exception) {
                Toast.makeText(
                    this@SelectRouteActivity,
                    "Error al cargar las rutas: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun openRoute(route: RouteEntity) {
        lifecycleScope.launch {
            try {
                if (route.status == "PAUSED") {
                    routeRepository.resumeRoute(route.id)
                }

                val intent = Intent(
                    this@SelectRouteActivity,
                    RouteResultActivity::class.java
                )

                intent.putExtra(
                    RouteResultActivity.EXTRA_ROUTE_ID,
                    route.id
                )

                startActivity(intent)
                finish()

            } catch (e: Exception) {
                Toast.makeText(
                    this@SelectRouteActivity,
                    "No se pudo abrir la ruta: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}