package com.routeplanner.app.data.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "destinations")
data class DestinationEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val address: String,

    val name: String? = null,

    val latitude: Double? = null,

    val longitude: Double? = null,

    val createdAt: Long = System.currentTimeMillis()
)