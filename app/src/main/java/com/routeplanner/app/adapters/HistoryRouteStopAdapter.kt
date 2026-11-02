package com.routeplanner.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.routeplanner.app.R
import com.routeplanner.app.data.database.entity.RouteStopEntity

class HistoryRouteStopAdapter :
    RecyclerView.Adapter<HistoryRouteStopAdapter.StopViewHolder>() {

    private val stops = mutableListOf<RouteStopEntity>()

    class StopViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvStopNumber: TextView =
            itemView.findViewById(R.id.tvHistoryStopNumber)

        val tvStopAddress: TextView =
            itemView.findViewById(R.id.tvHistoryStopAddress)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): StopViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history_route_stop, parent, false)

        return StopViewHolder(view)
    }

    override fun onBindViewHolder(holder: StopViewHolder, position: Int) {
        val stop = stops[position]

        holder.tvStopNumber.text = stop.stopOrder.toString()
        holder.tvStopAddress.text = stop.addressSnapshot
    }

    override fun getItemCount(): Int = stops.size

    fun updateStops(newStops: List<RouteStopEntity>) {
        stops.clear()
        stops.addAll(newStops.sortedBy { it.stopOrder })
        notifyDataSetChanged()
    }
}