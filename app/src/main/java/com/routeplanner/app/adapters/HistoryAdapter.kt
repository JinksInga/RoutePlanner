package com.routeplanner.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.routeplanner.app.R
import com.routeplanner.app.data.database.entity.RouteEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter(
    private val onViewRoute: (RouteEntity) -> Unit,
    private val onDeleteRoute: (RouteEntity) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private val routes = mutableListOf<RouteEntity>()

    class HistoryViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val tvRouteName: TextView =
            itemView.findViewById(R.id.tvHistoryRouteName)

        val tvRouteDate: TextView =
            itemView.findViewById(R.id.tvHistoryRouteDate)

        val tvRouteStops: TextView =
            itemView.findViewById(R.id.tvHistoryRouteStops)

        val btnViewRoute: Button =
            itemView.findViewById(R.id.btnViewHistoryRoute)

        val btnDeleteRoute: Button =
            itemView.findViewById(R.id.btnDeleteHistoryRoute)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HistoryViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_route, parent, false)

        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: HistoryViewHolder,
        position: Int
    ) {

        val route = routes[position]

        holder.tvRouteName.text = route.name

        val dateFormat = SimpleDateFormat(
            "dd/MM/yyyy HH:mm",
            Locale.getDefault()
        )

        holder.tvRouteDate.text =
            "Creada: ${dateFormat.format(Date(route.createdAt))}"

        holder.tvRouteStops.text = "Ruta finalizada"

        holder.btnViewRoute.setOnClickListener {
            onViewRoute(route)
        }

        holder.btnDeleteRoute.setOnClickListener {
            onDeleteRoute(route)
        }
    }

    override fun getItemCount(): Int = routes.size

    fun updateRoutes(newRoutes: List<RouteEntity>) {
        routes.clear()
        routes.addAll(newRoutes)
        notifyDataSetChanged()
    }
}