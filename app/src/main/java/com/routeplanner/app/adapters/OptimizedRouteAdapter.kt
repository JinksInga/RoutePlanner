
package com.routeplanner.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.routeplanner.app.R
import com.routeplanner.app.data.database.entity.RouteStopEntity

class OptimizedRouteAdapter(
    private val onCompleteStop: ((RouteStopEntity) -> Unit)? = null,
    private val showCompleteButton: Boolean = false,
    private val onNavigateToStop: ((RouteStopEntity) -> Unit)? = null
) : RecyclerView.Adapter<OptimizedRouteAdapter.RouteViewHolder>() {

    private val destinations = mutableListOf<RouteStopEntity>()

    // Referencias a los elementos de cada parada
    class RouteViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val card: com.google.android.material.card.MaterialCardView =
            itemView as com.google.android.material.card.MaterialCardView

        val tvStopNumber: TextView =
            itemView.findViewById(R.id.tvStopNumber)

        val tvStopCheck: TextView =
            itemView.findViewById(R.id.tvStopCheck)

        val tvStopAddress: TextView =
            itemView.findViewById(R.id.tvStopAddress)

        val btnCompleteStop: Button =
            itemView.findViewById(R.id.btnCompleteStop)
    }

    // Crear cada elemento de la lista
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RouteViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_optimized_route, parent, false)

        return RouteViewHolder(view)
    }

    // Mostrar los datos de cada parada
    override fun onBindViewHolder(
        holder: RouteViewHolder,
        position: Int
    ) {
        val destination = destinations[position]

        holder.tvStopNumber.text = destination.stopOrder.toString()
        holder.tvStopAddress.text = destination.addressSnapshot

        if (showCompleteButton) {
            // Parada pendiente
            holder.card.setCardBackgroundColor(
                android.graphics.Color.WHITE
            )

            holder.tvStopNumber.visibility = View.VISIBLE
            holder.tvStopCheck.visibility = View.GONE

            holder.tvStopNumber.setTextColor(
                android.graphics.Color.parseColor("#183153")
            )

            holder.tvStopAddress.setTextColor(
                android.graphics.Color.parseColor("#183153")
            )

            holder.tvStopAddress.paintFlags =
                holder.tvStopAddress.paintFlags and
                        android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()

        } else {
            // Parada completada
            holder.card.setCardBackgroundColor(
                android.graphics.Color.parseColor("#F0FDF4")
            )

            holder.tvStopNumber.visibility = View.VISIBLE
            holder.tvStopCheck.visibility = View.GONE

            holder.tvStopNumber.setTextColor(
                android.graphics.Color.parseColor("#15803D")
            )

            holder.tvStopAddress.paintFlags =
                holder.tvStopAddress.paintFlags or
                        android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
        }

        holder.btnCompleteStop.visibility =
            if (onCompleteStop != null) View.VISIBLE else View.GONE

        holder.btnCompleteStop.text =
            if (showCompleteButton) "Completar" else "Reabrir"

        holder.btnCompleteStop.setOnClickListener {
            onCompleteStop?.invoke(destination)
        }

        holder.itemView.setOnClickListener {
            if (showCompleteButton) {
                onNavigateToStop?.invoke(destination)
            }
        }
    }

    override fun getItemCount(): Int {
        return destinations.size
    }

    // Actualizar la lista de paradas
    fun updateDestinations(newDestinations: List<RouteStopEntity>) {
        destinations.clear()
        destinations.addAll(newDestinations)
        notifyDataSetChanged()
    }
}