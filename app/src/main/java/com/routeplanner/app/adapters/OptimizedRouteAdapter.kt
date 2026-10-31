package com.routeplanner.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.routeplanner.app.R
import com.routeplanner.app.data.database.entity.DestinationEntity

class OptimizedRouteAdapter :
    RecyclerView.Adapter<OptimizedRouteAdapter.RouteViewHolder>() {

    private val destinations = mutableListOf<DestinationEntity>()

    class RouteViewHolder(
        itemView: android.view.View
    ) : RecyclerView.ViewHolder(itemView) {

        val tvStopNumber: TextView =
            itemView.findViewById(R.id.tvStopNumber)

        val tvStopAddress: TextView =
            itemView.findViewById(R.id.tvStopAddress)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RouteViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_optimized_route, parent, false)

        return RouteViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: RouteViewHolder,
        position: Int
    ) {

        val destination = destinations[position]

        holder.tvStopNumber.text = (position + 1).toString()
        holder.tvStopAddress.text = destination.address
    }

    override fun getItemCount(): Int {
        return destinations.size
    }

    fun updateDestinations(newDestinations: List<DestinationEntity>) {
        destinations.clear()
        destinations.addAll(newDestinations)
        notifyDataSetChanged()
    }
}
/*
* ¿Qué hace el adaptador?
- Recibe la lista de destinos ordenados.
- Muestra el número de cada parada.
- Muestra su dirección.
- Actualiza la lista cuando recibe nuevos destinos.*/