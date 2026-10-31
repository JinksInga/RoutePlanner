package com.routeplanner.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.RecyclerView
import com.routeplanner.app.R
import com.routeplanner.app.data.database.entity.DestinationEntity

class RouteDestinationAdapter(
    private val onSelectionChanged: (List<DestinationEntity>) -> Unit
) : RecyclerView.Adapter<RouteDestinationAdapter.DestinationViewHolder>() {

    private val destinations = mutableListOf<DestinationEntity>()
    private val selectedIds = mutableSetOf<Int>()

    class DestinationViewHolder(
        val checkBox: CheckBox
    ) : RecyclerView.ViewHolder(checkBox)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DestinationViewHolder {
        val checkBox = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route_destination, parent, false) as CheckBox

        return DestinationViewHolder(checkBox)
    }

    override fun onBindViewHolder(
        holder: DestinationViewHolder,
        position: Int
    ) {
        val destination = destinations[position]

        holder.checkBox.setOnCheckedChangeListener(null)
        holder.checkBox.text = destination.address
        holder.checkBox.isChecked = selectedIds.contains(destination.id)

        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedIds.add(destination.id)
            } else {
                selectedIds.remove(destination.id)
            }

            onSelectionChanged(
                destinations.filter { selectedIds.contains(it.id) }
            )
        }
    }

    override fun getItemCount(): Int = destinations.size

    fun updateDestinations(newDestinations: List<DestinationEntity>) {
        destinations.clear()
        destinations.addAll(newDestinations)

        selectedIds.retainAll(destinations.map { it.id }.toSet())

        notifyDataSetChanged()

        onSelectionChanged(
            destinations.filter { selectedIds.contains(it.id) }
        )
    }

    fun restoreSelection(ids: Set<Int>) {
        selectedIds.clear()
        selectedIds.addAll(ids)

        notifyDataSetChanged()

        onSelectionChanged(
            destinations.filter { selectedIds.contains(it.id) }
        )
    }

}