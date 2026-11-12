package com.routeplanner.app.data.database.entity

import androidx.room.ColumnInfo
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

    @ColumnInfo(defaultValue = "0")
    val returnToOrigin: Boolean = false,

    @ColumnInfo(defaultValue = "0")
    val returnToOriginCompleted: Boolean = false,

    val createdAt: Long = System.currentTimeMillis(),

    val updatedAt: Long = System.currentTimeMillis()
)