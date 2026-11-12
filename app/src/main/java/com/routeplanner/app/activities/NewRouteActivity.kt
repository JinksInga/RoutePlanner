package com.routeplanner.app.activities

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
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
import com.routeplanner.app.R
import com.routeplanner.app.databinding.ActivityNewRouteBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import androidx.core.content.edit
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds

class NewRouteActivity : AppCompatActivity()
{
    companion object
    {
        const val EXTRA_ROUTE_NAME = "routeName"
        const val EXTRA_ORIGIN_LATITUDE = "originLatitude"
        const val EXTRA_ORIGIN_LONGITUDE = "originLongitude"
        const val EXTRA_ORIGIN_ADDRESS = "originAddress"
        const val EXTRA_OPTIMIZATION_TYPE = "optimizationType"
        const val EXTRA_RETURN_TO_ORIGIN = "returnToOrigin"
    }

    private lateinit var binding: ActivityNewRouteBinding
    private lateinit var fusedLocationClient:
            com.google.android.gms.location.FusedLocationProviderClient
    private lateinit var placesClient: PlacesClient
    private lateinit var optimizationOptions: List<String>
    private val AUTOCOMPLETE_ORIGIN_REQUEST_CODE = 1002
    private var cancellationTokenSource: CancellationTokenSource? = null

    // Coordenadas del punto de partida.
    private var selectedOriginLatitude: Double? = null
    private var selectedOriginLongitude: Double? = null
    private var selectedOriginAddress: String? = null

    private val preferences by lazy {
        getSharedPreferences("route_preferences", MODE_PRIVATE)
    }

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
                showBannerState(
                    getString(R.string.banner_message_location_permission_needed),
                    BannerType.WARNING)
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

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        optimizationOptions = this.resources.getStringArray(R.array.optimization_options_array).toList()

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

            preferences.edit {
                putString("optimization_type", optimizationType)
            }
        }
    }

    private fun loadOptimizationPreference()
    {
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
            binding.tilRouteName.error = getString(R.string.error_route_name_required)
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
            showBannerState(
                getString(R.string.banner_message_select_starting_point),
                BannerType.ERROR)
            return
        }

        // Recuperar el method de optimización.
        val optimizationType = preferences.getString(
            "optimization_type",
            "NEAREST"
        ) ?: "NEAREST"

        // Recuperar la opción de regreso al punto de partida.
        val returnToOrigin = binding.switchReturnToOrigin.isChecked

        // Guardar el nombre de la ruta.
        preferences.edit {
            putString("route_name", routeName)
        }

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
            putExtra(EXTRA_RETURN_TO_ORIGIN, returnToOrigin)
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
                AUTOCOMPLETE_ORIGIN_REQUEST_CODE)

        }
        catch (e: Exception)
        {
            showBannerState(getString(
                R.string.location_google_places_not_opened,
                e.message),
                BannerType.ERROR)
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

        showBannerState(
            getString(R.string.banner_message_getting_location),
            BannerType.LOADING)

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

                        selectedOriginAddress = address?: getString(R.string.location_gps_unavailable)

                        binding.tvSelectedOrigin.text = getString(
                            R.string.location_current_format,
                            selectedOriginAddress)
                        binding.bannerStatus.visibility = View.GONE

                        showBannerState(
                            getString(R.string.banner_message_location_success),
                            BannerType.SUCCESS)

                        lifecycleScope.launch {
                            delay(2000.milliseconds) // Espera 2 segundos
                            binding.bannerStatus.visibility = View.GONE
                        }
                    }
                }
                else
                {
                    showBannerState(
                        getString(R.string.banner_message_location_error),
                        BannerType.ERROR)
                }

            }.addOnFailureListener {
                showBannerState(getString(
                    R.string.location_error_retrieving,
                    it.localizedMessage),
                    BannerType.ERROR)
            }
        }
        catch (_: SecurityException)
        {
            showBannerState(
                getString(R.string.banner_message_permission_denied),
                BannerType.ERROR)
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

        if (resultCode != RESULT_OK || data == null) {
            return
        }

        val prediction = PlaceAutocomplete.getPredictionFromIntent(data)

        if (prediction == null)
        {
            showBannerState(
                getString(R.string.banner_message_address_not_found),
                BannerType.WARNING)
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

                if (location == null)
                {
                    showBannerState(
                        getString(R.string.banner_message_google_no_coordinates),
                        BannerType.WARNING)
                    return@addOnSuccessListener
                }

                selectedOriginLatitude = location.latitude
                selectedOriginLongitude = location.longitude
                selectedOriginAddress = address

                binding.tvSelectedOrigin.text = getString(
                    R.string.location_manual_format,
                    address)

                showBannerState(
                    getString(R.string.banner_message_starting_point_selected),
                    BannerType.SUCCESS)

                lifecycleScope.launch {
                    delay(2000.milliseconds) // Espera 2 segundos
                    binding.bannerStatus.visibility = View.GONE
                }
            }
            .addOnFailureListener { exception ->
                showBannerState(getString(
                        R.string.location_error_retrieving,
                    exception.message),
                    BannerType.ERROR)
            }
    }

    private fun showBannerState(message: String, type: BannerType) {
        binding.bannerStatus.visibility = View.VISIBLE
        binding.tvBannerMessage.text = message

        when (type)
        {
            BannerType.LOADING -> {
                binding.tvBannerMessage.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_loading_location_24, 0, 0, 0)
                binding.bannerStatus.setCardBackgroundColor(getColor(R.color.rp_primary)) }

            BannerType.SUCCESS -> {
                binding.tvBannerMessage.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_success_location_24, 0, 0, 0)
                binding.bannerStatus.setCardBackgroundColor(getColor(R.color.rp_success))}

            BannerType.WARNING -> {
                binding.tvBannerMessage.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_warning_location_24, 0, 0, 0)
                binding.bannerStatus.setCardBackgroundColor(getColor(R.color.rp_warning))}

            BannerType.ERROR -> {
                binding.tvBannerMessage.setCompoundDrawablesRelativeWithIntrinsicBounds(
                    R.drawable.ic_error_location_24, 0, 0, 0)
                binding.bannerStatus.setCardBackgroundColor(getColor(R.color.md_theme_onErrorContainer)) }
        }
    }

    // Enum para clasificar los estados
    enum class BannerType {
        LOADING, SUCCESS, WARNING, ERROR
    }

    override fun onDestroy() {
        cancellationTokenSource?.cancel()
        super.onDestroy()
    }

}