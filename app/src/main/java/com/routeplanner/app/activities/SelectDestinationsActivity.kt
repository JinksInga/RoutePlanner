package com.routeplanner.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.routeplanner.app.R
import com.routeplanner.app.RouteOptimizer
import com.routeplanner.app.adapters.RouteDestinationAdapter
import com.routeplanner.app.data.database.RouteDatabase
import com.routeplanner.app.data.database.entity.RouteEntity
import com.routeplanner.app.data.database.entity.RouteStopEntity
import com.routeplanner.app.databinding.ActivitySelectDestinationsBinding
import com.routeplanner.app.repository.DestinationRepository
import com.routeplanner.app.repository.RouteRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SelectDestinationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySelectDestinationsBinding
    private lateinit var adapter: RouteDestinationAdapter
    private lateinit var repository: DestinationRepository
    private lateinit var routeRepository: RouteRepository

    private val preferences by lazy {
        getSharedPreferences("route_preferences", MODE_PRIVATE)
    }

    // Datos recibidos desde NewRouteActivity.
    private var routeName: String = ""
    private var originLatitude: Double = 0.0
    private var originLongitude: Double = 0.0
    private var originAddress: String = ""
    private var optimizationType: String = "NEAREST"
    private var returnToOrigin: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySelectDestinationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Recuperar los datos de la primera pantalla.
        if (!receiveRouteData()) {
            finish()
            return
        }

        configureToolbar()
        configureRepositories()
        configureRecyclerView()
        observeDestinations()
        configureButtons()
    }

    private fun receiveRouteData(): Boolean {

        routeName = intent.getStringExtra(
            NewRouteActivity.EXTRA_ROUTE_NAME
        ) ?: ""

        originAddress = intent.getStringExtra(
            NewRouteActivity.EXTRA_ORIGIN_ADDRESS
        ) ?: ""

        originLatitude = intent.getDoubleExtra(
            NewRouteActivity.EXTRA_ORIGIN_LATITUDE,
            Double.NaN
        )

        originLongitude = intent.getDoubleExtra(
            NewRouteActivity.EXTRA_ORIGIN_LONGITUDE,
            Double.NaN
        )

        optimizationType = intent.getStringExtra(
            NewRouteActivity.EXTRA_OPTIMIZATION_TYPE
        ) ?: "NEAREST"

        returnToOrigin = intent.getBooleanExtra(
            NewRouteActivity.EXTRA_RETURN_TO_ORIGIN,
            false
        )

        if (routeName.isBlank() ||
            originAddress.isBlank() ||
            !originLatitude.isFinite() ||
            !originLongitude.isFinite()
        ) {
            Toast.makeText(
                this,
                "Faltan datos del punto de partida o de la ruta.",
                Toast.LENGTH_LONG
            ).show()

            return false
        }

        return true
    }

    private fun configureToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun configureRepositories() {

        val database = RouteDatabase.getDatabase(applicationContext)

        repository = DestinationRepository(
            database.destinationDao()
        )

        routeRepository = RouteRepository(
            routeDao = database.routeDao(),
            routeStopDao = database.routeStopDao(),
            database = database
        )
    }

    private fun configureRecyclerView() {

        adapter = RouteDestinationAdapter { selectedDestinations ->

            val selectedIds = selectedDestinations.map {
                it.id
            }.toSet()

            saveSelectedDestinations(selectedIds)

            val count = selectedDestinations.size

            binding.tvDestinationCount.apply {
                text = when (count) {
                    1 -> "1 destino seleccionado"
                    else -> "$count destinos seleccionados"
                }

                setTextColor(
                    ContextCompat.getColor(
                        this@SelectDestinationsActivity,
                        R.color.rp_text_primary
                    )
                )
            }
        }

        binding.rvRouteDestinations.layoutManager =
            LinearLayoutManager(this)

        binding.rvRouteDestinations.adapter = adapter
    }

    private fun observeDestinations() {

        lifecycleScope.launch {

            repository.getAllDestinations().collectLatest { destinations ->

                // Recuperar la selección antes de actualizar el adaptador.
                val savedSelection = getSelectedDestinations()

                adapter.updateDestinations(destinations)
                adapter.restoreSelection(savedSelection)

                if (destinations.isEmpty()) {

                    binding.tvNoDestinations.visibility = View.VISIBLE
                    binding.rvRouteDestinations.visibility = View.GONE

                } else {

                    binding.tvNoDestinations.visibility = View.GONE
                    binding.rvRouteDestinations.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun configureButtons() {

        binding.btnCalculateRoute.setOnClickListener {
            calculateRoute()
        }

        binding.btnAddDestination.setOnClickListener {

            val intent = Intent(
                this,
                DestinationsActivity::class.java
            ).apply {
                putExtra(
                    DestinationsActivity.EXTRA_RETURN_AFTER_ADD,
                    true
                )
            }

            addDestinationLauncher.launch(intent)
        }
    }

    private fun saveSelectedDestinations(ids: Set<Int>) {

        preferences.edit()
            .putStringSet(
                "selected_destination_ids",
                ids.map { it.toString() }.toSet()
            )
            .apply()
    }

    private fun getSelectedDestinations(): Set<Int> {

        return preferences.getStringSet(
            "selected_destination_ids",
            emptySet()
        )?.mapNotNull {
            it.toIntOrNull()
        }?.toSet() ?: emptySet()
    }

    private fun calculateRoute() {

        val selectedIds = getSelectedDestinations()

        if (selectedIds.size < 2) {
            binding.tvDestinationCount.apply {
                text = "Selecciona al menos dos destinos."
                setTextColor(
                    ContextCompat.getColor(
                        this@SelectDestinationsActivity,
                        R.color.md_theme_error
                    )
                )
            }
            return
        }

        // La optimización global todavía no está implementada.
        if (optimizationType == "GLOBAL") {

            Toast.makeText(
                this,
                "La optimización global todavía no está disponible.",
                Toast.LENGTH_LONG
            ).show()

            return
        }

        lifecycleScope.launch {

            binding.btnCalculateRoute.isEnabled = false

            try {

                // Recuperar las direcciones seleccionadas.
                val allDestinations =
                    repository.getAllDestinations().first()

                val selectedDestinations =
                    allDestinations.filter {
                        selectedIds.contains(it.id)
                    }

                if (selectedDestinations.isEmpty()) {

                    Toast.makeText(
                        this@SelectDestinationsActivity,
                        "No se han encontrado los destinos seleccionados.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@launch
                }

                // Comprobar que todas las direcciones tienen coordenadas.
                val missingCoordinates =
                    selectedDestinations.any {
                        it.latitude == null || it.longitude == null
                    }

                if (missingCoordinates) {

                    Toast.makeText(
                        this@SelectDestinationsActivity,
                        "Hay destinos sin coordenadas. Revisa sus direcciones.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@launch
                }

                // Calcular el orden de visita.
                val optimizedRoute =
                    RouteOptimizer.optimizeNearestNeighbor(
                        originLatitude,
                        originLongitude,
                        selectedDestinations
                    )

                if (optimizedRoute.size != selectedDestinations.size) {

                    Toast.makeText(
                        this@SelectDestinationsActivity,
                        "No se han podido ordenar todos los destinos.",
                        Toast.LENGTH_LONG
                    ).show()

                    return@launch
                }

                // Crear la ruta.
                val route = RouteEntity(
                    name = routeName,
                    originAddress = originAddress,
                    originLatitude = originLatitude,
                    originLongitude = originLongitude,
                    optimizationType = "NEAREST",
                    returnToOrigin = returnToOrigin
                )

                // Crear las paradas según el orden optimizado.
                val stops = optimizedRoute.mapIndexed { index, destination ->

                    RouteStopEntity(
                        routeId = 0,
                        destinationId = destination.id,
                        addressSnapshot = destination.address,
                        latitude = destination.latitude!!,
                        longitude = destination.longitude!!,
                        stopOrder = index + 1
                    )
                }

                // Guardar la ruta y sus paradas.
                val routeId = routeRepository.createRouteWithStops(
                    route = route,
                    stops = stops
                )

                Toast.makeText(
                    this@SelectDestinationsActivity,
                    "Ruta guardada correctamente.",
                    Toast.LENGTH_SHORT
                ).show()

                // Abrir los resultados.
                val resultIntent = Intent(
                    this@SelectDestinationsActivity,
                    RouteResultActivity::class.java
                )

                resultIntent.putExtra(
                    RouteResultActivity.EXTRA_DESTINATION_IDS,
                    optimizedRoute.map { it.id }.toIntArray()
                )

                resultIntent.putExtra(
                    RouteResultActivity.EXTRA_ROUTE_ID,
                    routeId.toInt()
                )

                startActivity(resultIntent)
                finish()

            } catch (e: Exception) {

                Toast.makeText(
                    this@SelectDestinationsActivity,
                    "Error al calcular la ruta: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()

            } finally {

                binding.btnCalculateRoute.isEnabled = true
            }
        }
    }

    private val addDestinationLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode == RESULT_OK) {

                val destinationId = result.data?.getIntExtra(
                    DestinationsActivity.EXTRA_CREATED_DESTINATION_ID,
                    -1
                ) ?: -1

                if (destinationId != -1) {

                    val selectedIds = getSelectedDestinations().toMutableSet()
                    selectedIds.add(destinationId)

                    saveSelectedDestinations(selectedIds)

                    lifecycleScope.launch {
                        val destinations = repository
                            .getAllDestinations()
                            .first()

                        adapter.updateDestinations(destinations)
                        adapter.restoreSelection(selectedIds)
                    }
                }
            }
        }

}