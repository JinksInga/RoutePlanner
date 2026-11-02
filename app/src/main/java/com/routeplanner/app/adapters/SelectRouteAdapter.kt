package com.routeplanner.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.routeplanner.app.R
import com.routeplanner.app.data.database.entity.RouteEntity

data class RouteSelectionItem(
    val route: RouteEntity,
    val totalStops: Int,
    val completedStops: Int
)

class SelectRouteAdapter(
    private val onSelectRoute: (RouteEntity) -> Unit
) : RecyclerView.Adapter<SelectRouteAdapter.RouteViewHolder>() {

    private val routes = mutableListOf<RouteSelectionItem>()

    class RouteViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val tvRouteName: TextView =
            itemView.findViewById(R.id.tvRouteName)

        val tvRouteStatus: TextView =
            itemView.findViewById(R.id.tvRouteStatus)

        val tvRouteProgress: TextView =
            itemView.findViewById(R.id.tvRouteProgress)

        val btnSelectRoute: Button =
            itemView.findViewById(R.id.btnSelectRoute)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RouteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_select_route, parent, false)

        return RouteViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: RouteViewHolder,
        position: Int
    ) {
        val item = routes[position]
        val route = item.route

        holder.tvRouteName.text = route.name

        holder.tvRouteStatus.text =
            if (route.status == "ACTIVE") {
                "En curso"
            } else {
                "Pausada"
            }

        holder.tvRouteProgress.text =
            "Paradas completadas: ${item.completedStops} de ${item.totalStops}"

        holder.btnSelectRoute.text =
            if (route.status == "ACTIVE") {
                "Abrir ruta"
            } else {
                "Reanudar ruta"
            }

        holder.btnSelectRoute.setOnClickListener {
            onSelectRoute(route)
        }

        holder.itemView.setOnClickListener {
            onSelectRoute(route)
        }
    }

    override fun getItemCount(): Int = routes.size

    fun updateRoutes(newRoutes: List<RouteSelectionItem>) {
        routes.clear()
        routes.addAll(newRoutes)
        notifyDataSetChanged()
    }
}