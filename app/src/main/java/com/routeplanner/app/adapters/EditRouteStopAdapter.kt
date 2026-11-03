package com.routeplanner.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.routeplanner.app.R
import com.routeplanner.app.data.database.entity.RouteStopEntity

class EditRouteStopAdapter(
    private val onMoveUp: (RouteStopEntity) -> Unit,
    private val onMoveDown: (RouteStopEntity) -> Unit,
    private val onRemove: (RouteStopEntity) -> Unit
) : RecyclerView.Adapter<EditRouteStopAdapter.StopViewHolder>() {

    private val stops = mutableListOf<RouteStopEntity>()

    class StopViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val tvNumber: TextView =
            itemView.findViewById(R.id.tvEditStopNumber)

        val tvAddress: TextView =
            itemView.findViewById(R.id.tvEditStopAddress)

        val tvStatus: TextView =
            itemView.findViewById(R.id.tvEditStopStatus)

        val btnMoveUp: Button =
            itemView.findViewById(R.id.btnMoveStopUp)

        val btnMoveDown: Button =
            itemView.findViewById(R.id.btnMoveStopDown)

        val btnRemove: Button =
            itemView.findViewById(R.id.btnRemoveStop)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): StopViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_edit_route_stop, parent, false)

        return StopViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: StopViewHolder,
        position: Int
    ) {
        val stop = stops[position]
        val completed = stop.isCompleted

        holder.tvNumber.text = "Parada ${position + 1}"
        holder.tvAddress.text = stop.addressSnapshot
        holder.tvStatus.text =
            if (completed) "Completada" else "Pendiente"

        // Las paradas completadas no se pueden modificar.
        holder.btnMoveUp.isEnabled = !completed && position > 0
        holder.btnMoveDown.isEnabled =
            !completed && position < stops.lastIndex

        holder.btnRemove.isEnabled = !completed

        holder.btnMoveUp.setOnClickListener {
            onMoveUp(stop)
        }

        holder.btnMoveDown.setOnClickListener {
            onMoveDown(stop)
        }

        holder.btnRemove.setOnClickListener {
            onRemove(stop)
        }
    }

    override fun getItemCount(): Int = stops.size

    fun updateStops(newStops: List<RouteStopEntity>) {
        stops.clear()
        stops.addAll(newStops)
        notifyDataSetChanged()
    }
}