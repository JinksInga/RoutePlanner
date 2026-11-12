package com.routeplanner.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.PlaceAutocomplete
import com.routeplanner.app.BuildConfig
import com.routeplanner.app.R
import com.routeplanner.app.adapters.EditRouteStopAdapter
import com.routeplanner.app.data.database.RouteDatabase
import com.routeplanner.app.data.database.entity.DestinationEntity
import com.routeplanner.app.data.database.entity.RouteStopEntity
import com.routeplanner.app.databinding.ActivityEditRouteBinding
import com.routeplanner.app.repository.DestinationRepository
import com.routeplanner.app.repository.RouteRepository
import com.routeplanner.app.repository.RouteStopRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class EditRouteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditRouteBinding
    private lateinit var routeRepository: RouteRepository
    private lateinit var routeStopRepository: RouteStopRepository
    private lateinit var adapter: EditRouteStopAdapter
    private lateinit var destinationRepository: DestinationRepository
    private lateinit var placesClient: PlacesClient
    private val AUTOCOMPLETE_REQUEST_CODE = 1003
    private var routeId: Int = -1
    private val editedStops = mutableListOf<RouteStopEntity>()

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)

        binding = ActivityEditRouteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(
                applicationContext,
                BuildConfig.PLACES_API_KEY
            )
        }
        placesClient = Places.createClient(this)

        routeId = intent.getIntExtra(
            RouteResultActivity.EXTRA_ROUTE_ID,
            -1
        )

        if (routeId == -1) {
            Toast.makeText(this, "Ruta no válida", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val database = RouteDatabase.getDatabase(applicationContext)

        routeRepository = RouteRepository(
            routeDao = database.routeDao(),
            routeStopDao = database.routeStopDao(),
            database = database
        )

        routeStopRepository = RouteStopRepository(database.routeStopDao())
        destinationRepository = DestinationRepository(database.destinationDao())

        configureRecyclerView()

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }

        binding.fabAddRouteStop.setOnClickListener {
            showAddStopOptions()
        }

        loadRoute()
    }

    private fun showAddStopOptions() {
        val options = arrayOf(
            "Elegir dirección guardada",
            "Buscar una dirección nueva"
        )

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Añadir parada")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showSavedDestinations()
                    1 -> launchNewDestinationAutocomplete()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun launchNewDestinationAutocomplete() {
        val intent = PlaceAutocomplete.createIntent(this) {
            setCountries(listOf("ES"))
        }

        startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
    }

    private fun showSavedDestinations() {
        lifecycleScope.launch {
            try {
                val destinations = destinationRepository
                    .getAllDestinations()
                    .first()

                if (destinations.isEmpty()) {
                    Toast.makeText(
                        this@EditRouteActivity,
                        "No tienes direcciones guardadas.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                val availableDestinations = destinations.filter {
                    it.latitude != null && it.longitude != null
                }

                if (availableDestinations.isEmpty()) {
                    Toast.makeText(
                        this@EditRouteActivity,
                        "No hay direcciones con coordenadas disponibles.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                val addresses = availableDestinations
                    .map { it.address }
                    .toTypedArray()

                androidx.appcompat.app.AlertDialog.Builder(
                    this@EditRouteActivity
                )
                    .setTitle("Selecciona una dirección")
                    .setItems(addresses) { _, position ->
                        addSavedDestination(
                            availableDestinations[position]
                        )
                    }
                    .setNegativeButton("Cancelar", null)
                    .show()

            } catch (e: Exception) {
                Toast.makeText(
                    this@EditRouteActivity,
                    "Error al cargar direcciones: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun addSavedDestination(destination: DestinationEntity) {

        // Evitar añadir dos veces la misma dirección.
        val alreadyAdded = editedStops.any {
            it.destinationId == destination.id
        }

        if (alreadyAdded) {
            Toast.makeText(
                this,
                "Esta dirección ya está en la ruta.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val nextOrder = (editedStops.maxOfOrNull {
            it.stopOrder
        } ?: 0) + 1

        val newStop = RouteStopEntity(
            id = 0,
            routeId = routeId,
            destinationId = destination.id,
            addressSnapshot = destination.address,
            latitude = destination.latitude!!,
            longitude = destination.longitude!!,
            stopOrder = nextOrder,
            isCompleted = false,
            completedAt = null
        )

        editedStops.add(newStop)
        adapter.updateStops(editedStops)

        Toast.makeText(
            this,
            "Dirección añadida. Pulsa Guardar cambios para conservarla.",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun configureRecyclerView() {
        adapter = EditRouteStopAdapter(
            onMoveUp = { stop -> moveStop(stop, -1) },
            onMoveDown = { stop -> moveStop(stop, 1) },
            onRemove = { stop -> removeStop(stop) }
        )

        binding.rvEditRouteStops.layoutManager = LinearLayoutManager(this)
        binding.rvEditRouteStops.adapter = adapter
    }

    private fun loadRoute() {
        lifecycleScope.launch {
            try {
                val route = routeRepository.getRouteById(routeId)

                if (route == null || route.status == "FINISHED") {
                    Toast.makeText(
                        this@EditRouteActivity,
                        "Esta ruta no se puede modificar.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    return@launch
                }

                binding.etEditRouteName.setText(route.name)
                binding.tvEditRouteOrigin.text =
                    "Origen: ${route.originAddress}"

                val stops = routeStopRepository
                    .getStopsByRoute(routeId)
                    .first()

                editedStops.clear()
                editedStops.addAll(stops)

                adapter.updateStops(editedStops)

            } catch (e: Exception) {
                Toast.makeText(
                    this@EditRouteActivity,
                    "Error al cargar la ruta: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun moveStop(stop: RouteStopEntity, direction: Int) {
        val currentIndex = editedStops.indexOfFirst { it.id == stop.id }

        if (currentIndex == -1) return

        val newIndex = currentIndex + direction

        if (newIndex !in editedStops.indices) return

        if (editedStops[currentIndex].isCompleted ||
            editedStops[newIndex].isCompleted
        ) {
            Toast.makeText(
                this,
                "No puedes cambiar la posición de una parada completada.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val current = editedStops[currentIndex]

        editedStops[currentIndex] = editedStops[newIndex]
        editedStops[newIndex] = current

        adapter.updateStops(editedStops)
    }

    private fun removeStop(stop: RouteStopEntity) {
        editedStops.removeAll { it.id == stop.id }
        adapter.updateStops(editedStops)
    }

    private fun saveChanges() {
        lifecycleScope.launch {
            try {
                val route = routeRepository.getRouteById(routeId)

                if (route == null || route.status == "FINISHED") {
                    Toast.makeText(
                        this@EditRouteActivity,
                        "Esta ruta ya no se puede modificar.",
                        Toast.LENGTH_LONG
                    ).show()
                    finish()
                    return@launch
                }

                val newRouteName = binding.etEditRouteName.text
                    ?.toString()
                    ?.trim()
                    .orEmpty()

                if (newRouteName.isBlank()) {
                    binding.tilEditRouteName.error =
                        getString(R.string.error_route_name_required)
                    binding.etEditRouteName.requestFocus()
                    return@launch
                }

                binding.tilEditRouteName.error = null

                val completedStops = routeStopRepository
                    .getStopsByRoute(routeId)
                    .first()
                    .filter { it.isCompleted }

                val currentCompletedIds = completedStops.map { it.id }.toSet()
                val editedCompletedIds = editedStops
                    .filter { it.isCompleted }
                    .map { it.id }
                    .toSet()

                if (currentCompletedIds != editedCompletedIds) {
                    Toast.makeText(
                        this@EditRouteActivity,
                        "No se pueden eliminar paradas completadas.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                val originalStops = routeStopRepository
                    .getStopsByRoute(routeId)
                    .first()

                val editedIds = editedStops.map { it.id }.toSet()

                originalStops
                    .filter { it.id !in editedIds && !it.isCompleted }
                    .forEach { routeStopRepository.delete(it) }

                editedStops.forEachIndexed { index, stop ->
                    val updatedStop = stop.copy(
                        stopOrder = index + 1
                    )

                    if (updatedStop.id == 0) {
                        routeStopRepository.insert(updatedStop)
                    } else {
                        routeStopRepository.update(updatedStop)
                    }
                }

                // Actualizar el nombre de la ruta
                routeRepository.update(
                    route.copy(
                        name = newRouteName,
                        updatedAt = System.currentTimeMillis()
                    )
                )

                Toast.makeText(
                    this@EditRouteActivity,
                    "Cambios guardados correctamente.",
                    Toast.LENGTH_SHORT
                ).show()

                finish()

            } catch (e: Exception) {
                Toast.makeText(
                    this@EditRouteActivity,
                    "Error al guardar los cambios: ${e.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == AUTOCOMPLETE_REQUEST_CODE &&
            resultCode == RESULT_OK &&
            data != null
        ) {
            val prediction = PlaceAutocomplete.getPredictionFromIntent(data)

            if (prediction == null) {
                Toast.makeText(
                    this,
                    "No se pudo obtener la dirección seleccionada.",
                    Toast.LENGTH_LONG
                ).show()
                return
            }

            val sessionToken = PlaceAutocomplete.getSessionTokenFromIntent(data)

            val request = FetchPlaceRequest.builder(
                prediction.placeId,
                listOf(
                    Place.Field.FORMATTED_ADDRESS,
                    Place.Field.LOCATION
                )
            )
                .setSessionToken(sessionToken)
                .build()

            placesClient.fetchPlace(request)
                .addOnSuccessListener { response ->
                    val place = response.place
                    val location = place.location
                    val address = place.formattedAddress

                    if (location != null && address != null) {
                        addNewPlaceStop(
                            address,
                            location.latitude,
                            location.longitude
                        )
                    } else {
                        Toast.makeText(
                            this,
                            "No se pudo obtener la ubicación de la dirección.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
                .addOnFailureListener {
                    Toast.makeText(
                        this,
                        "Error al obtener la dirección.",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }

    private fun addNewPlaceStop(
        address: String,
        latitude: Double,
        longitude: Double
    ) {
        val newStop = RouteStopEntity(
            id = 0,
            routeId = routeId,
            destinationId = null,
            addressSnapshot = address,
            latitude = latitude,
            longitude = longitude,
            stopOrder = (editedStops.maxOfOrNull { it.stopOrder } ?: 0) + 1,
            isCompleted = false,
            completedAt = null
        )

        editedStops.add(newStop)
        adapter.updateStops(editedStops)

        Toast.makeText(
            this,
            "Nueva parada añadida. Pulsa Guardar cambios para conservarla.",
            Toast.LENGTH_LONG
        ).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_edit_route, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_save_route -> {
                saveChanges()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

}