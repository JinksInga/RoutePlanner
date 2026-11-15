package com.routeplanner.app.data.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "route_stops",
    foreignKeys = [
        ForeignKey(
            entity = RouteEntity::class,
            parentColumns = ["id"],
            childColumns = ["routeId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["routeId"]),
        Index(value = ["destinationId"])
    ]
)
data class RouteStopEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val routeId: Int,

    val destinationId: Int?,

    val addressSnapshot: String,

    val nameSnapshot: String? = null,

    val latitude: Double,

    val longitude: Double,

    val stopOrder: Int,

    val isCompleted: Boolean = false,

    val completedAt: Long? = null
)