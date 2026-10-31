package com.routeplanner.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "destinations")
data class DestinationEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val address: String,

    val latitude: Double? = null,

    val longitude: Double? = null,

    val createdAt: Long = System.currentTimeMillis()
)
/*
* Algunos detalles:
- id: identificador automático.
- address: dirección completa.
- latitude: latitud geográfica.
- longitude: longitud geográfica.
- createdAt: fecha de creación.
* */