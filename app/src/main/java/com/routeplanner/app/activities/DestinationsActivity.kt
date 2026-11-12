package com.routeplanner.app.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.routeplanner.app.data.database.RouteDatabase
import com.routeplanner.app.data.database.entity.DestinationEntity
import com.routeplanner.app.databinding.ActivityDestinationsBinding
import com.routeplanner.app.repository.DestinationRepository
import com.routeplanner.app.adapters.DestinationAdapter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import com.routeplanner.app.BuildConfig
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.PlaceAutocomplete

class DestinationsActivity : Activity()
{
    companion object {
        const val EXTRA_RETURN_AFTER_ADD = "returnAfterAdd"
        const val EXTRA_CREATED_DESTINATION_ID = "createdDestinationId"
    }

    private lateinit var binding: ActivityDestinationsBinding
    private lateinit var repository: DestinationRepository
    private lateinit var adapter: DestinationAdapter
    private lateinit var placesClient: PlacesClient
    private var pendingDestination: DestinationEntity? = null
    private val AUTOCOMPLETE_REQUEST_CODE = 1001

    private val activityScope =
        CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var allDestinations =
        listOf<DestinationEntity>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDestinationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (!Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(
                applicationContext,
                BuildConfig.PLACES_API_KEY
            )
        }

        placesClient = Places.createClient(this)

        val database = RouteDatabase.getDatabase(this)

        repository = DestinationRepository(
            database.destinationDao()
        )

        configureRecyclerView()
        configureButtons()
        configureSearch()
        observeDestinations()

        if (intent.getBooleanExtra(EXTRA_RETURN_AFTER_ADD, false)) {
            launchNewDestinationAutocomplete()
        }
    }

    private fun launchNewDestinationAutocomplete() {
        launchAutocomplete(null)
    }

    private fun configureRecyclerView() {
        adapter = DestinationAdapter(
            onEdit = { destination ->
                launchAutocomplete(destination)
            },
            onDelete = { destination ->
                confirmDelete(destination)
            }
        )

        binding.rvDestinations.layoutManager = LinearLayoutManager(this)
        binding.rvDestinations.adapter = adapter
    }

    private fun configureButtons() {

        binding.btnAddDestination.setOnClickListener {
            launchAutocomplete(null)
        }
    }


    private fun launchAutocomplete(destination: DestinationEntity?) {
        pendingDestination = destination

        try {
            val intent = PlaceAutocomplete.createIntent(this) {
                setCountries(listOf("ES"))

                // Si estamos editando, cargar la dirección anterior.
                setInitialQuery(destination?.address ?: "")
            }

            startActivityForResult(
                intent,
                AUTOCOMPLETE_REQUEST_CODE
            )

        } catch (e: Exception) {
            Toast.makeText(
                this,
                "No se pudo abrir Google Places: ${e.message}",
                Toast.LENGTH_LONG
            ).show()
        }
    }


    private fun observeDestinations() {

        activityScope.launch {
            repository.getAllDestinations().collect { destinations ->

                allDestinations = destinations

                filterDestinations(
                    binding.etSearch.text.toString()
                )
            }
        }
    }

    private fun configureSearch() {

        binding.etSearch.addTextChangedListener(
            object : TextWatcher {

                override fun beforeTextChanged(
                    s: CharSequence?,
                    start: Int,
                    count: Int,
                    after: Int
                ) {}

                override fun onTextChanged(
                    s: CharSequence?,
                    start: Int,
                    before: Int,
                    count: Int
                ) {
                    filterDestinations(s.toString())
                }

                override fun afterTextChanged(s: Editable?) {}
            }
        )
    }

    private fun filterDestinations(query: String) {

        val filtered = allDestinations.filter {
            it.address.contains(
                query,
                ignoreCase = true
            )
        }

        adapter.updateData(filtered)

        binding.tvEmpty.visibility =
            if (filtered.isEmpty()) android.view.View.VISIBLE
            else android.view.View.GONE

        binding.rvDestinations.visibility =
            if (filtered.isEmpty()) android.view.View.GONE
            else android.view.View.VISIBLE

        binding.tvEmpty.text =
            if (allDestinations.isEmpty()) {
                "Todavía no tienes direcciones guardadas."
            } else {
                "No se encontraron direcciones."
            }
    }

    private fun showDestinationDialog(
        destination: DestinationEntity?
    ) {

        val input = EditText(this)

        input.hint = "Introduce una dirección"
        input.setSingleLine(true)

        if (destination != null) {
            input.setText(destination.address)
            input.setSelection(input.text.length)
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle(
                if (destination == null) {
                    "Nueva dirección"
                } else {
                    "Modificar dirección"
                }
            )
            .setView(input)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Guardar", null)
            .create()

        dialog.setOnShowListener {

            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener {

                    val address = input.text.toString().trim()

                    if (address.isBlank()) {
                        input.error = "Introduce una dirección"
                        return@setOnClickListener
                    }

                    activityScope.launch {

                        if (destination == null) {

                            repository.insert(
                                DestinationEntity(
                                    address = address
                                )
                            )

                            Toast.makeText(
                                this@DestinationsActivity,
                                "Dirección guardada",
                                Toast.LENGTH_SHORT
                            ).show()

                        } else {

                            repository.update(
                                destination.copy(
                                    address = address
                                )
                            )

                            Toast.makeText(
                                this@DestinationsActivity,
                                "Dirección modificada",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        dialog.dismiss()
                    }
                }
        }

        dialog.show()
    }

    private fun confirmDelete(
        destination: DestinationEntity
    ) {

        AlertDialog.Builder(this)
            .setTitle("Eliminar dirección")
            .setMessage(
                "¿Deseas eliminar esta dirección?\n\n" +
                        destination.address
            )
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Eliminar") { _, _ ->

                activityScope.launch {

                    repository.delete(destination)

                    Toast.makeText(
                        this@DestinationsActivity,
                        "Dirección eliminada",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .show()
    }

    override fun onDestroy()
    {
        super.onDestroy()
        activityScope.cancel()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int,
                                  resultCode: Int,
                                  data: android.content.Intent?)
    {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode != AUTOCOMPLETE_REQUEST_CODE) {
            return
        }

        if (resultCode != Activity.RESULT_OK || data == null) {
            return
        }

        val prediction = PlaceAutocomplete.getPredictionFromIntent(data)

        if (prediction == null) {
            Toast.makeText(
                this,
                "No se pudo identificar el lugar seleccionado",
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

                saveSelectedDestination(
                    address,
                    location.latitude,
                    location.longitude
                )
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Error al consultar el lugar: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun saveSelectedDestination(address: String,
                                        latitude: Double,
                                        longitude: Double)
    {
        activityScope.launch {

            val destination = pendingDestination

            if (destination == null) {

                val newDestinationId = repository.insert(
                    DestinationEntity(
                        address = address,
                        latitude = latitude,
                        longitude = longitude
                    )
                )

                Toast.makeText(
                    this@DestinationsActivity,
                    "Dirección guardada correctamente",
                    Toast.LENGTH_SHORT
                ).show()

                if (intent.getBooleanExtra(EXTRA_RETURN_AFTER_ADD, false)) {
                    val resultIntent = Intent().apply {
                        putExtra(
                            EXTRA_CREATED_DESTINATION_ID,
                            newDestinationId.toInt()
                        )
                    }

                    setResult(RESULT_OK, resultIntent)
                    finish()
                }

            } else {

                repository.update(
                    destination.copy(
                        address = address,
                        latitude = latitude,
                        longitude = longitude
                    )
                )

                Toast.makeText(
                    this@DestinationsActivity,
                    "Dirección modificada correctamente",
                    Toast.LENGTH_SHORT
                ).show()
            }

            pendingDestination = null
        }
    }
}