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

    val latitude: Double,

    val longitude: Double,

    val stopOrder: Int,

    val isCompleted: Boolean = false,

    val completedAt: Long? = null
)
/*
* ¿Qué hemos conseguido?
- routeId relaciona la parada con su ruta.
- destinationId permite localizar la dirección original,
  pero puede ser nulo si esa dirección se elimina.
- addressSnapshot conserva la dirección tal como era cuando se creó la ruta.
- latitude y longitude conservan sus coordenadas originales.
- stopOrder mantiene el orden calculado.
- isCompleted registra si has confirmado la visita.
- completedAt guarda cuándo se completó.
* Además, CASCADE garantiza que, si se elimina una ruta, también se eliminen sus paradas,
  pero no las direcciones guardadas.
* */