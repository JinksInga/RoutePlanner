package com.routeplanner.app.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.routeplanner.app.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    private val preferences by lazy {
        getSharedPreferences("route_preferences", MODE_PRIVATE)
    }

    private val KEY_NAVIGATION_APP = "navigation_app"
    private val KEY_OPTIMIZATION_TYPE = "optimization_type"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configureToolbar()
        loadNavigationPreference()
        configureNavigationSelection()
        loadOptimizationPreference()
        configureOptimizationSelection()
    }

    private fun loadOptimizationPreference() {
        val optimizationType = preferences.getString(
            KEY_OPTIMIZATION_TYPE,
            "NEAREST"
        )

        when (optimizationType) {
            "GLOBAL" -> binding.rbGlobal.isChecked = true
            else -> binding.rbNearest.isChecked = true
        }
    }

    private fun configureOptimizationSelection() {
        binding.rgOptimization.setOnCheckedChangeListener { _, checkedId ->

            val selectedOptimization = when (checkedId) {
                binding.rbNearest.id -> "NEAREST"
                binding.rbGlobal.id -> "GLOBAL"
                else -> return@setOnCheckedChangeListener
            }

            preferences.edit()
                .putString(KEY_OPTIMIZATION_TYPE, selectedOptimization)
                .apply()
        }
    }

    private fun configureToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun loadNavigationPreference() {
        val navigationApp = preferences.getString(
            KEY_NAVIGATION_APP,
            "GOOGLE_MAPS"
        )

        when (navigationApp) {
            "WAZE" -> binding.rbWaze.isChecked = true
            else -> binding.rbGoogleMaps.isChecked = true
        }
    }

    private fun configureNavigationSelection() {
        binding.rgNavigationApp.setOnCheckedChangeListener { _, checkedId ->

            val selectedApp = when (checkedId) {
                binding.rbWaze.id -> "WAZE"
                binding.rbGoogleMaps.id -> "GOOGLE_MAPS"
                else -> return@setOnCheckedChangeListener
            }

            preferences.edit()
                .putString(KEY_NAVIGATION_APP, selectedApp)
                .apply()
        }
    }
}