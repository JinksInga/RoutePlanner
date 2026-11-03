package com.routeplanner.app.activities

import android.app.Activity
import android.content.Intent
import com.routeplanner.app.BuildConfig
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.PlaceAutocomplete
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.routeplanner.app.adapters.RouteDestinationAdapter
import com.routeplanner.app.data.database.RouteDatabase
import com.routeplanner.app.databinding.ActivityNewRouteBinding
import com.routeplanner.app.repository.DestinationRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import com.routeplanner.app.RouteOptimizer
import kotlinx.coroutines.flow.first
import kotlin.jvm.java
import com.routeplanner.app.data.database.entity.RouteEntity
import com.routeplanner.app.data.database.entity.RouteStopEntity
import com.routeplanner.app.repository.RouteRepository
import android.location.Geocoder
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NewRouteActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNewRouteBinding
    private lateinit var adapter: RouteDestinationAdapter
    private lateinit var repository: DestinationRepository
    private lateinit var routeRepository: RouteRepository
    private lateinit var fusedLocationClient:
            com.google.android.gms.location.FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient

    private val AUTOCOMPLETE_ORIGIN_REQUEST_CODE = 1002
    private var cancellationTokenSource: CancellationTokenSource? = null
    private var selectedCount = 0

    // Coordenadas del punto de partida
    private var selectedOriginLatitude: Double? = null
    private var selectedOriginLongitude: Double? = null
    private var selectedOriginAddress: String? = null

    private val preferences by lazy {
        getSharedPreferences("route_preferences", MODE_PRIVATE)
    }

    // Solicitud de permisos de ubicación
    private val locationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->

            val fineGranted =
                permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true

            val coarseGranted =
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (fineGranted || coarseGranted) {
                getCurrentLocation()
            } else {
                Toast.makeText(
                    this,
                    "Necesitamos permiso de ubicación para obtener tu posición.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityNewRouteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(
                applicationContext,
                BuildConfig.PLACES_API_KEY
            )
        }

        placesClient = Places.createClient(this)

        fusedLocationClient =
            LocationServices.getFusedLocationProviderClient(this)

        configureRepository()
        configureRecyclerView()
        observeDestinations()
        configureButtons()

        loadOptimizationPreference()
        checkLocationPermission()
    }

    //Así tendremos acceso tanto a las direcciones como a las rutas y sus paradas.
    private fun configureRepository() {

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

            selectedCount = selectedDestinations.size

            saveSelectedDestinations(
                selectedDestinations.map { it.id }.toSet()
            )

            binding.tvDestinationCount.text =
                if (selectedCount == 1) {
                    "1 destino seleccionado"
                } else {
                    "$selectedCount destinos seleccionados"
                }
        }

        binding.rvRouteDestinations.layoutManager =
            LinearLayoutManager(this)

        binding.rvRouteDestinations.adapter = adapter
    }

    private fun observeDestinations() {
        lifecycleScope.launch {
            repository.getAllDestinations().collectLatest { destinations ->

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

        binding.btnCurrentLocation.setOnClickListener {
            checkLocationPermission()
        }

        binding.btnManualOrigin.setOnClickListener {
            launchOriginAutocomplete()
        }

        binding.btnCalculateRoute.setOnClickListener {
            calculateRoute()
        }
    }

    private fun loadOptimizationPreference() {

        val optimizationType = preferences.getString(
            "optimization_type",
            "NEAREST"
        )

        when (optimizationType) {
            "GLOBAL" -> binding.rbGlobal.isChecked = true
            else -> binding.rbNearest.isChecked = true
        }
    }

    private fun calculateRoute() {

        val originLatitude = selectedOriginLatitude
        val originLongitude = selectedOriginLongitude
        val originAddress = selectedOriginAddress

        // Comprobar el origen.
        if (originLatitude == null ||
            originLongitude == null ||
            originAddress == null
        ) {
            Toast.makeText(
                this,
                "Primero debes seleccionar un punto de partida.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Comprobar los destinos seleccionados.
        val selectedIds = getSelectedDestinations()

        if (selectedIds.isEmpty()) {
            Toast.makeText(
                this,
                "Selecciona al menos un destino.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        // Recuperar el method de optimización guardado.
        val optimizationType = getSharedPreferences(
            "route_preferences",
            MODE_PRIVATE
        ).getString("optimization_type", "NEAREST")

        // Comprobar si Google Routes API está seleccionado.
        if (optimizationType == "GLOBAL") {
            Toast.makeText(
                this,
                "La optimización global todavía no está disponible.",
                Toast.LENGTH_LONG
            ).show()
            return
        }

        lifecycleScope.launch {

            try {
                // Recuperar los destinos guardados.
                val allDestinations =
                    repository.getAllDestinations().first()

                val selectedDestinations =
                    allDestinations.filter {
                        selectedIds.contains(it.id)
                    }

                if (selectedDestinations.isEmpty()) {
                    Toast.makeText(
                        this@NewRouteActivity,
                        "No se han encontrado los destinos seleccionados.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                // Comprobar coordenadas.
                val missingCoordinates =
                    selectedDestinations.any {
                        it.latitude == null || it.longitude == null
                    }

                if (missingCoordinates) {
                    Toast.makeText(
                        this@NewRouteActivity,
                        "Hay destinos sin coordenadas. Revisa sus direcciones.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                // Calcular el recorrido optimizado.
                val optimizedRoute =
                    RouteOptimizer.optimizeNearestNeighbor(
                        originLatitude,
                        originLongitude,
                        selectedDestinations
                    )

                if (optimizedRoute.size != selectedDestinations.size) {
                    Toast.makeText(
                        this@NewRouteActivity,
                        "No se han podido ordenar todos los destinos.",
                        Toast.LENGTH_LONG
                    ).show()
                    return@launch
                }

                // Generar nombre para la ruta.
                val routeName = binding.etRouteName.text
                    .toString()
                    .trim()

                if (routeName.isBlank()) {
                    Toast.makeText(
                        this@NewRouteActivity,
                        "Introduce un nombre para la ruta.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@launch
                }

                // Crear la entidad de ruta.
                val route = RouteEntity(
                    name = routeName,
                    originAddress =  selectedOriginAddress ?: "Ubicación actual",//originAddress,
                    originLatitude = originLatitude,
                    originLongitude = originLongitude,
                    optimizationType = "NEAREST"
                )

                // Crear las paradas respetando el orden calculado.
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

                // Guardar la ruta y todas sus paradas.
                val routeId = routeRepository.createRouteWithStops(
                    route = route,
                    stops = stops
                )

                Toast.makeText(
                    this@NewRouteActivity,
                    "Ruta guardada correctamente.",
                    Toast.LENGTH_SHORT
                ).show()

                // Abrir la pantalla de resultados.
                val intent = Intent(
                    this@NewRouteActivity,
                    RouteResultActivity::class.java
                )

                // Conservamos temporalmente los IDs para que la pantalla
                // de resultados actual siga funcionando.
                intent.putExtra(
                    RouteResultActivity.EXTRA_DESTINATION_IDS,
                    optimizedRoute.map { it.id }.toIntArray()
                )

                // También enviamos el identificador de la nueva ruta.
                intent.putExtra(
                    RouteResultActivity.EXTRA_ROUTE_ID,
                    routeId.toInt()
                )

                startActivity(intent)
                finish()

            } catch (e: Exception) {

                Toast.makeText(
                    this@NewRouteActivity,
                    "Error al guardar la ruta: ${e.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun launchOriginAutocomplete() {

        try {
            val intent = PlaceAutocomplete.createIntent(this) {
                setCountries(listOf("ES"))
            }

            startActivityForResult(
                intent,
                AUTOCOMPLETE_ORIGIN_REQUEST_CODE
            )

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "No se pudo abrir Google Places: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun checkLocationPermission() {

        val fineGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            getCurrentLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }


    private fun getCurrentLocation() {

        cancellationTokenSource?.cancel()
        cancellationTokenSource = CancellationTokenSource()

        Toast.makeText(
            this,
            "Obteniendo ubicación...",
            Toast.LENGTH_SHORT
        ).show()

        try {
            fusedLocationClient.getCurrentLocation(
                Priority.PRIORITY_HIGH_ACCURACY,
                cancellationTokenSource!!.token
            ).addOnSuccessListener { location ->

                if (location != null) {

                    selectedOriginLatitude = location.latitude
                    selectedOriginLongitude = location.longitude

                    lifecycleScope.launch {

                        val address = withContext(Dispatchers.IO) {
                            try {
                                val geocoder = Geocoder(
                                    this@NewRouteActivity,
                                    Locale("es", "ES")
                                )

                                @Suppress("DEPRECATION")
                                geocoder.getFromLocation(
                                    location.latitude,
                                    location.longitude,
                                    1
                                )?.firstOrNull()?.getAddressLine(0)

                            } catch (e: Exception) {
                                null
                            }
                        }

                        selectedOriginAddress = address
                            ?: "Ubicación GPS (dirección no disponible)"

                        binding.tvSelectedOrigin.text =
                            "Punto de partida:\n$selectedOriginAddress"

                        Toast.makeText(
                            this@NewRouteActivity,
                            "Ubicación obtenida correctamente.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                } else {
                    Toast.makeText(
                        this,
                        "No se pudo obtener la ubicación. Comprueba que el GPS está activado.",
                        Toast.LENGTH_LONG
                    ).show()
                }

            }.addOnFailureListener {
                Toast.makeText(
                    this,
                    "Error al obtener la ubicación: ${it.localizedMessage}",
                    Toast.LENGTH_LONG
                ).show()
            }

        } catch (e: SecurityException) {
            Toast.makeText(
                this,
                "No tenemos permiso para acceder a la ubicación.",
                Toast.LENGTH_LONG
            ).show()
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
        )?.mapNotNull { it.toIntOrNull() }?.toSet() ?: emptySet()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != AUTOCOMPLETE_ORIGIN_REQUEST_CODE) {
            return
        }

        if (resultCode != Activity.RESULT_OK || data == null) {
            return
        }

        val prediction = PlaceAutocomplete.getPredictionFromIntent(data)

        if (prediction == null) {
            Toast.makeText(
                this,
                "No se pudo identificar la dirección seleccionada",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val sessionToken =
            PlaceAutocomplete.getSessionTokenFromIntent(data)

        val placeFields = listOf(
            Place.Field.FORMATTED_ADDRESS,
            Place.Field.LOCATION
        )

        val request = FetchPlaceRequest.builder(
            prediction.placeId,
            placeFields
        )
            .setSessionToken(sessionToken)
            .build()

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->

                val place = response.place

                val address = place.formattedAddress
                    ?: prediction.getFullText(null).toString()

                val location = place.location

                if (location == null) {
                    Toast.makeText(
                        this,
                        "Google no ha devuelto las coordenadas",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@addOnSuccessListener
                }

                selectedOriginLatitude = location.latitude
                selectedOriginLongitude = location.longitude

                selectedOriginAddress = address

                binding.tvSelectedOrigin.text =
                    "$address\n" +
                            "Latitud: ${location.latitude}\n" +
                            "Longitud: ${location.longitude}"

                Toast.makeText(
                    this,
                    "Punto de partida seleccionado",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { exception ->

                Toast.makeText(
                    this,
                    "Error al consultar el lugar: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    override fun onDestroy() {
        cancellationTokenSource?.cancel()
        super.onDestroy()
    }
}