package com.routeplanner.app.activities

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.PlaceAutocomplete
import com.routeplanner.app.BuildConfig
import com.routeplanner.app.databinding.ActivityNewRouteBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class NewRouteActivity : AppCompatActivity()
{
    companion object
    {
        const val EXTRA_ROUTE_NAME = "routeName"
        const val EXTRA_ORIGIN_LATITUDE = "originLatitude"
        const val EXTRA_ORIGIN_LONGITUDE = "originLongitude"
        const val EXTRA_ORIGIN_ADDRESS = "originAddress"
        const val EXTRA_OPTIMIZATION_TYPE = "optimizationType"
    }

    private lateinit var binding: ActivityNewRouteBinding
    private lateinit var fusedLocationClient:
            com.google.android.gms.location.FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient

    private val AUTOCOMPLETE_ORIGIN_REQUEST_CODE = 1002
    private var cancellationTokenSource: CancellationTokenSource? = null

    // Coordenadas del punto de partida.
    private var selectedOriginLatitude: Double? = null
    private var selectedOriginLongitude: Double? = null
    private var selectedOriginAddress: String? = null

    private val preferences by lazy {
        getSharedPreferences("route_preferences", MODE_PRIVATE)
    }

    private val optimizationOptions = listOf(
        "Vecino más cercano",
        "Optimización global (Google Routes)"
    )

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

        configureToolbar()
        configureOptimizationDropdown()
        loadOptimizationPreference()
        configureButtons()

        checkLocationPermission()
    }

    private fun configureToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun configureOptimizationDropdown() {

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_dropdown_item_1line,
            optimizationOptions
        )

        binding.actOptimization.setAdapter(adapter)

        binding.actOptimization.setOnItemClickListener { _, _, position, _ ->

            val optimizationType = if (position == 1) {
                "GLOBAL"
            } else {
                "NEAREST"
            }

            preferences.edit()
                .putString("optimization_type", optimizationType)
                .apply()
        }
    }

    private fun loadOptimizationPreference() {

        val optimizationType = preferences.getString(
            "optimization_type",
            "NEAREST"
        )

        val selectedOption = if (optimizationType == "GLOBAL") {
            optimizationOptions[1]
        } else {
            optimizationOptions[0]
        }

        binding.actOptimization.setText(selectedOption, false)
    }

    private fun configureButtons() {

        binding.btnCurrentLocation.setOnClickListener {
            checkLocationPermission()
        }

        binding.btnManualOrigin.setOnClickListener {
            launchOriginAutocomplete()
        }

        binding.btnSelectDestinations.setOnClickListener {
            continueToDestinations()
        }
    }

    private fun continueToDestinations() {

        // Validar el nombre de la ruta.
        val routeName = binding.etRouteName.text
            ?.toString()
            ?.trim()
            .orEmpty()

        if (routeName.isBlank()) {
            binding.tilRouteName.error = "Introduce un nombre para la ruta."
            binding.etRouteName.requestFocus()
            return
        }

        binding.tilRouteName.error = null

        // Validar el punto de partida.
        val originLatitude = selectedOriginLatitude
        val originLongitude = selectedOriginLongitude
        val originAddress = selectedOriginAddress

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

        // Recuperar el method de optimización.
        val optimizationType = preferences.getString(
            "optimization_type",
            "NEAREST"
        ) ?: "NEAREST"

        // Guardar el nombre de la ruta.
        preferences.edit()
            .putString("route_name", routeName)
            .apply()

        // Abrir la pantalla de selección de destinos.
        val intent = Intent(
            this,
            SelectDestinationsActivity::class.java
        ).apply {

            putExtra(EXTRA_ROUTE_NAME, routeName)
            putExtra(EXTRA_ORIGIN_LATITUDE, originLatitude)
            putExtra(EXTRA_ORIGIN_LONGITUDE, originLongitude)
            putExtra(EXTRA_ORIGIN_ADDRESS, originAddress)
            putExtra(EXTRA_OPTIMIZATION_TYPE, optimizationType)
        }

        startActivity(intent)
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

                            } catch (_: Exception) {
                                null
                            }
                        }

                        selectedOriginAddress = address
                            ?: "Ubicación GPS (dirección no disponible)"

                        binding.tvSelectedOrigin.text = selectedOriginAddress

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

        } catch (_: SecurityException) {
            Toast.makeText(
                this,
                "No tenemos permiso para acceder a la ubicación.",
                Toast.LENGTH_LONG
            ).show()
        }
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

                binding.tvSelectedOrigin.text = address

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