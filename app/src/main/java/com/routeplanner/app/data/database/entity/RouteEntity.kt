package com.routeplanner.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "routes")
data class RouteEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val name: String,

    val originAddress: String,

    val originLatitude: Double,

    val originLongitude: Double,

    val status: String = "ACTIVE",

    val optimizationType: String,

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis()
)
/*
* ¿Qué guarda cada campo?
- id: identificador único de la ruta.
- name: nombre que tendrá el recorrido.
- originAddress: dirección desde donde comienza.
- originLatitude y originLongitude: coordenadas del origen.
- status: estado de la ruta (ACTIVE, PAUSED o FINISHED).
- optimizationType: algoritmo utilizado (NEAREST o GLOBAL).
- createdAt: fecha de creación.
- updatedAt: fecha de la última modificación.
* */