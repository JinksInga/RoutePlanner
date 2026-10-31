package com.routeplanner.app.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.routeplanner.app.data.database.entity.DestinationEntity
import com.routeplanner.app.databinding.ItemDestinationBinding

class DestinationAdapter(
    private val onEdit: (DestinationEntity) -> Unit,
    private val onDelete: (DestinationEntity) -> Unit
) : RecyclerView.Adapter<DestinationAdapter.DestinationViewHolder>() {

    private var destinations = listOf<DestinationEntity>()

    inner class DestinationViewHolder(
        val binding: ItemDestinationBinding
    ) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DestinationViewHolder {

        val binding = ItemDestinationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )

        return DestinationViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: DestinationViewHolder,
        position: Int
    ) {

        val destination = destinations[position]

        holder.binding.tvAddress.text = destination.address

        holder.binding.btnEdit.setOnClickListener {
            onEdit(destination)
        }

        holder.binding.btnDelete.setOnClickListener {
            onDelete(destination)
        }
    }

    override fun getItemCount(): Int {
        return destinations.size
    }

    fun updateData(newDestinations: List<DestinationEntity>) {
        destinations = newDestinations
        notifyDataSetChanged()
    }
}