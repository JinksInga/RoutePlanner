package com.routeplanner.app.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.routeplanner.app.adapters.OptimizedRouteAdapter
import com.routeplanner.app.data.database.RouteDatabase
import com.routeplanner.app.databinding.ActivityRouteResultBinding
import com.routeplanner.app.repository.DestinationRepository
import kotlinx.coroutines.launch

class RouteResultActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_DESTINATION_IDS = "extra_destination_ids"
    }

    private lateinit var binding: ActivityRouteResultBinding
    private lateinit var adapter: OptimizedRouteAdapter
    private lateinit var repository: DestinationRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityRouteResultBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = RouteDatabase.getDatabase(applicationContext)
        repository = DestinationRepository(database.destinationDao())

        configureRecyclerView()
        loadRoute()

        binding.btnBackToNewRoute.setOnClickListener {
            finish()
        }

        binding.btnStartRoute.setOnClickListener {
            Toast.makeText(
                this,
                "La navegación se implementará en la siguiente fase.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun configureRecyclerView() {
        adapter = OptimizedRouteAdapter()

        binding.rvOptimizedRoute.layoutManager =
            LinearLayoutManager(this)

        binding.rvOptimizedRoute.adapter = adapter
    }

    private fun loadRoute() {
        val destinationIds =
            intent.getIntArrayExtra(EXTRA_DESTINATION_IDS)

        if (destinationIds == null || destinationIds.isEmpty()) {
            Toast.makeText(
                this,
                "No se han recibido destinos.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        lifecycleScope.launch {
            val destinations = mutableListOf<com.routeplanner.app.data.database.entity.DestinationEntity>()

            for (id in destinationIds) {
                val destination = repository.getDestination(id)

                if (destination != null) {
                    destinations.add(destination)
                }
            }

            if (destinations.isEmpty()) {
                Toast.makeText(
                    this@RouteResultActivity,
                    "No se han podido recuperar las paradas.",
                    Toast.LENGTH_LONG
                ).show()
                finish()
                return@launch
            }

            adapter.updateDestinations(destinations)

            binding.tvRouteSummary.text =
                "${destinations.size} paradas en tu recorrido"
        }
    }
}