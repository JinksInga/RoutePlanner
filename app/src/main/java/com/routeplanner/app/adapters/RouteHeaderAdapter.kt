package com.routeplanner.app.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.routeplanner.app.R

class RouteHeaderAdapter(
    private val title: String
) : RecyclerView.Adapter<RouteHeaderAdapter.HeaderViewHolder>() {

    private var itemCount = 0

    class HeaderViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {

        val tvHeader: TextView =
            itemView.findViewById(R.id.tvRouteHeader)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HeaderViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_route_header, parent, false)

        return HeaderViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: HeaderViewHolder,
        position: Int
    ) {
        holder.tvHeader.text = "$title ($itemCount)"
    }

    fun updateCount(newCount: Int) {
        val oldCount = itemCount
        itemCount = newCount

        when {
            oldCount == 0 && newCount > 0 -> notifyItemInserted(0)
            oldCount > 0 && newCount == 0 -> notifyItemRemoved(0)
            oldCount > 0 && newCount > 0 -> notifyItemChanged(0)
        }
    }

    override fun getItemCount(): Int = if (itemCount > 0) 1 else 0
}