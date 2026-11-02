package com.routeplanner.app.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.routeplanner.app.adapters.HistoryRouteStopAdapter
import com.routeplanner.app.data.database.RouteDatabase
import com.routeplanner.app.databinding.ActivityHistoryRouteBinding
import com.routeplanner.app.repository.RouteRepository
import com.routeplanner.app.repository.RouteStopRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HistoryRouteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryRouteBinding
    private lateinit var routeRepository: RouteRepository
    private lateinit var routeStopRepository: RouteStopRepository
    private lateinit var adapter: HistoryRouteStopAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHistoryRouteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configureToolbar()
        configureRepositories()
        configureRecyclerView()
        loadRoute()
    }

    private fun configureToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun configureRepositories() {
        val database = RouteDatabase.getDatabase(applicationContext)

        routeRepository = RouteRepository(
            routeDao = database.routeDao(),
            routeStopDao = database.routeStopDao(),
            database = database
        )

        routeStopRepository = RouteStopRepository(
            database.routeStopDao()
        )
    }

    private fun configureRecyclerView() {
        adapter = HistoryRouteStopAdapter()

        binding.rvHistoryRouteStops.layoutManager =
            LinearLayoutManager(this)

        binding.rvHistoryRouteStops.adapter = adapter
    }

    private fun loadRoute() {
        val routeId = intent.getIntExtra(
            RouteResultActivity.EXTRA_ROUTE_ID,
            -1
        )

        if (routeId == -1) {
            Toast.makeText(
                this,
                "No se ha encontrado la ruta",
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        }

        lifecycleScope.launch {
            val route = routeRepository.getRouteById(routeId)

            if (route == null) {
                Toast.makeText(
                    this@HistoryRouteActivity,
                    "No se ha encontrado la ruta",
                    Toast.LENGTH_SHORT
                ).show()
                finish()
                return@launch
            }

            binding.toolbar.title = route.name
            binding.tvHistoryRouteName.text = route.name
            binding.tvHistoryRouteOrigin.text =
                "Origen: ${route.originAddress}"

            routeStopRepository.getStopsByRoute(routeId)
                .collectLatest { stops ->
                    adapter.updateStops(stops)
                }
        }
    }
}