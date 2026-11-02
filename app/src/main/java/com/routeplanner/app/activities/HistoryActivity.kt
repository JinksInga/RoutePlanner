package com.routeplanner.app.activities

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.routeplanner.app.adapters.HistoryAdapter
import com.routeplanner.app.data.database.RouteDatabase
import com.routeplanner.app.databinding.ActivityHistoryBinding
import com.routeplanner.app.repository.RouteRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HistoryActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoryBinding
    private lateinit var adapter: HistoryAdapter
    private lateinit var routeRepository: RouteRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityHistoryBinding.inflate(layoutInflater)
        setContentView(binding.root)

        configureToolbar()
        configureRepository()
        configureRecyclerView()
        observeFinishedRoutes()
    }

    private fun configureToolbar() {
        setSupportActionBar(binding.toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun configureRepository() {
        val database = RouteDatabase.getDatabase(applicationContext)

        routeRepository = RouteRepository(
            routeDao = database.routeDao(),
            routeStopDao = database.routeStopDao(),
            database = database
        )
    }

    private fun configureRecyclerView() {
        adapter = HistoryAdapter { route ->
            val intent = Intent(this,HistoryRouteActivity::class.java)
            intent.putExtra(RouteResultActivity.EXTRA_ROUTE_ID,route.id)
            startActivity(intent)
        }

        binding.rvHistory.layoutManager =
            LinearLayoutManager(this)

        binding.rvHistory.adapter = adapter
    }

    private fun observeFinishedRoutes() {
        lifecycleScope.launch {
            routeRepository.getFinishedRoutes()
                .collectLatest { routes ->

                    adapter.updateRoutes(routes)

                    val isEmpty = routes.isEmpty()

                    binding.tvEmptyHistory.visibility =
                        if (isEmpty) View.VISIBLE else View.GONE

                    binding.rvHistory.visibility =
                        if (isEmpty) View.GONE else View.VISIBLE
                }
        }
    }
}